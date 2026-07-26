package dev.anvilcraft.lib.v2.sync.management;

import com.google.common.collect.MapMaker;
import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import dev.anvilcraft.lib.v2.sync.annotation.LazySync;
import dev.anvilcraft.lib.v2.sync.network.payload.LazySyncPayload;
import dev.anvilcraft.lib.v2.sync.network.payload.LazySyncPayload.FieldChange;
import dev.anvilcraft.lib.v2.sync.util.SideUtil;
import dev.anvilcraft.lib.v2.sync.util.SyncDirection;
import dev.anvilcraft.lib.v2.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 惰性同步管理器。
 *
 * <p>负责被 {@link LazySync} 标注字段的「扫描 - 差分 - 分组同步」：</p>
 * <ul>
 *   <li>实例 / 静态目标由字节码注入在 {@code <init>} / {@code <clinit>} 中登记
 *       （见 {@link dev.anvilcraft.lib.v2.sync.transform.LazySyncBytecodeInjector}）。</li>
 *   <li>每 tick 结束时（{@code ServerTickEvent.Post} / {@code ClientTickEvent.Post}）
 *       遍历目标，将每个字段的当前值用其编解码器编码为字节，与上一快照对比；变更的字段
 *       按所属对象分组，每个对象仅发送一个 {@link LazySyncPayload}。</li>
 *   <li>收到分组包时按对象写回各字段，并同步更新本侧快照以抑制回环。</li>
 * </ul>
 *
 * <p>采用「编码为字节再比较」的方式检测变更，可统一处理可变值（如 {@code CompoundTag}、
 * {@code ItemStack}）的原地修改，且复用网络线格式，代价是每 tick 对每个被跟踪字段编码一次。</p>
 */
@Slf4j
@ApiStatus.Internal
public class LazySyncManager {
    /**
     * 单侧状态：被跟踪的实例 / 静态目标及其字段快照。
     *
     * <p>跟踪集合使用弱引用 + 标识比较（{@link MapMaker#weakKeys()}），使卸载的对象可被 GC，
     * 无需显式反注册。</p>
     */
    private static final class Side {
        final Set<Object> instances = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
        final Set<Class<?>> statics = ConcurrentHashMap.newKeySet();
        /** object -> (configKey -> 上一次编码的字节快照) */
        final Map<Object, Map<String, byte[]>> snapshots = new MapMaker().weakKeys().makeMap();
    }

    private final Side server = new Side();
    private final Side client = new Side();

    /** runtime class -> 实例 {@link LazySync} 字段元数据（含继承）。 */
    private final Map<Class<?>, List<LazyFieldMeta>> instanceMetaCache = new ConcurrentHashMap<>();
    /** owner class -> 静态 {@link LazySync} 字段元数据（含继承）。 */
    private final Map<Class<?>, List<LazyFieldMeta>> staticMetaCache = new ConcurrentHashMap<>();

    /**
     * 单个 {@link LazySync} 字段的运行时元数据。
     *
     * @param field     反射字段（已 setAccessible）
     * @param codec     字段值的编解码器
     * @param direction 同步方向
     * @param configKey 配置键 {@code DeclaringClassName#fieldName}
     */
    private record LazyFieldMeta(
        Field field,
        StreamCodec<? extends ByteBuf, Object> codec,
        SyncDirection direction,
        String configKey
    ) {
    }

    /**
     * 一个待写回的字段，由 {@link LazySyncPayload} 解包后传入 {@link #applyGrouped}。
     *
     * @param configKey 配置键 {@code DeclaringClassName#fieldName}
     * @param blob      字段值负载
     */
    public record AppliedField(String configKey, byte[] blob) {
    }

    private Side side() {
        return Util.isServer() ? this.server : this.client;
    }

    /**
     * 登记一个实例目标。由字节码注入在每个 {@code <init>} 末尾调用。
     *
     * @param object 被 {@link LazySync} 标注字段所属的实例
     */
    @ApiStatus.Internal
    public void track(@Nullable Object object) {
        if (object == null) return;
        this.side().instances.add(object);
    }

    /**
     * 登记一个静态目标。由字节码注入在 {@code <clinit>} 末尾调用。
     *
     * @param owner 含静态 {@link LazySync} 字段的类
     */
    @ApiStatus.Internal
    public void trackStatic(@Nullable Class<?> owner) {
        if (owner == null) return;
        this.side().statics.add(owner);
    }

    /**
     * 服务端 tick：扫描所有服务端跟踪的目标并下发变更。由 {@code ServerTickEvent.Post} 驱动。
     */
    public void tickServer() {
        this.tickAll(this.server, true);
    }

    /**
     * 客户端 tick：扫描所有客户端跟踪的目标并上行变更。由 {@code ClientTickEvent.Post} 驱动。
     */
    public void tickClient() {
        this.tickAll(this.client, false);
    }

    private void tickAll(Side side, boolean serverSide) {
        if (!side.instances.isEmpty()) {
            // 复制以避免在 tick 期间新对象登记导致的并发修改
            for (Object obj : List.copyOf(side.instances)) {
                List<LazyFieldMeta> metas = this.resolveInstanceMeta(obj.getClass());
                if (metas.isEmpty()) continue;
                this.scanTarget(side, serverSide, obj, metas);
            }
        }
        for (Class<?> owner : side.statics) {
            List<LazyFieldMeta> metas = this.resolveStaticMeta(owner);
            if (metas.isEmpty()) continue;
            this.scanTarget(side, serverSide, owner, metas);
        }
    }

    /**
     * 扫描单个目标的全部 {@link LazySync} 字段，编码后与快照对比，将本侧可发送的变更分组发送。
     *
     * <p>首次观测某字段时仅建立基线、不发送（与 {@link dev.anvilcraft.lib.v2.sync.annotation.Sync @Sync}
     * 一致：初始值在两端构造时即相同，只有后续变更才需要同步）。</p>
     */
    private void scanTarget(Side side, boolean serverSide, Object target, List<LazyFieldMeta> metas) {
        Map<String, byte[]> snap = side.snapshots.computeIfAbsent(target, k -> new HashMap<>());
        List<FieldChange> changes = new ArrayList<>();
        for (LazyFieldMeta meta : metas) {
            boolean canSend = serverSide ? meta.direction().isCreateByServer() : meta.direction().isCreateByClient();
            if (!canSend) continue;
            Object value;
            try {
                value = meta.field().get(Modifier.isStatic(meta.field().getModifiers()) ? null : target);
            } catch (IllegalAccessException e) {
                log.warn("Cannot read @LazySync field {}", meta.configKey(), e);
                continue;
            }
            byte[] encoded = this.encodeField(meta, value);
            byte[] prev = snap.get(meta.configKey());
            if (prev != null && Arrays.equals(prev, encoded)) continue;
            snap.put(meta.configKey(), encoded);
            if (prev == null) continue; // 首次观测：仅建立基线
            changes.add(new FieldChange(meta.configKey(), encoded));
        }
        if (!changes.isEmpty()) {
            this.sendGrouped(target, changes);
        }
    }

    private void sendGrouped(Object target, List<FieldChange> changes) {
        SyncRegisterEntry<Object, Object> entry = AnvilLibSync.SYNC_MANAGER.contains(target.getClass());
        if (entry == null) {
            log.warn("LazySync target {} is not registered for syncing", target.getClass().getName());
            return;
        }
        Object id = entry.idGetter().apply(target);
        String lookupClassName = target.getClass().getName();
        SideUtil.send(
            SyncDirection.BOTH, // 字段已按方向预过滤，此处恒从当前侧发送
            entry.dimension(),
            entry.dimensionGetter(),
            target,
            id,
            (flow) -> LazySyncPayload.create(entry.idCodec(), id, lookupClassName, changes, flow)
        );
    }

    /**
     * 写回一个对象的分组变更，并同步更新本侧快照以抑制回环。由 {@link LazySyncPayload} 收包后调用。
     *
     * @param object 解析出的目标对象（静态字段时为所属 {@link Class}）
     * @param fields 待写回的字段
     */
    @ApiStatus.Internal
    public void applyGrouped(Object object, List<AppliedField> fields) {
        boolean isStatic = object instanceof Class<?>;
        List<LazyFieldMeta> metas = isStatic
                                        ? this.resolveStaticMeta((Class<?>) object)
                                        : this.resolveInstanceMeta(object.getClass());
        Map<String, LazyFieldMeta> byKey = new HashMap<>();
        for (LazyFieldMeta meta : metas) byKey.put(meta.configKey(), meta);

        Map<String, byte[]> snap = this.side().snapshots.computeIfAbsent(object, k -> new HashMap<>());
        for (AppliedField af : fields) {
            LazyFieldMeta meta = byKey.get(af.configKey());
            if (meta == null) {
                log.warn("Received @LazySync field {} not found on {}", af.configKey(), object);
                continue;
            }
            Object value = this.decodeField(meta, af.blob());
            try {
                meta.field().set(isStatic ? null : object, value);
            } catch (IllegalAccessException e) {
                log.warn("Cannot write @LazySync field {}", af.configKey(), e);
                continue;
            }
            // 以写回值重新编码作为快照，保证下一 tick 差分不会把这次写回当作本侧变更重新上行/下发
            snap.put(af.configKey(), this.encodeField(meta, value));
        }
    }

    private byte[] encodeField(LazyFieldMeta meta, @Nullable Object value) {
        FriendlyByteBuf buf = SideUtil.createFriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(value == null);
        if (value != null) {
            Util.<StreamCodec<FriendlyByteBuf, Object>>cast(meta.codec()).encode(buf, value);
        }
        byte[] array = new byte[buf.readableBytes()];
        buf.readBytes(array);
        return array;
    }

    private @Nullable Object decodeField(LazyFieldMeta meta, byte[] blob) {
        FriendlyByteBuf buf = SideUtil.createFriendlyByteBuf(Unpooled.buffer());
        buf.writeBytes(blob);
        boolean isNull = buf.readBoolean();
        if (isNull) return null;
        return Util.<StreamCodec<FriendlyByteBuf, Object>>cast(meta.codec()).decode(buf);
    }

    private List<LazyFieldMeta> resolveInstanceMeta(Class<?> clazz) {
        return this.instanceMetaCache.computeIfAbsent(clazz, c -> this.buildMeta(c, false));
    }

    private List<LazyFieldMeta> resolveStaticMeta(Class<?> clazz) {
        return this.staticMetaCache.computeIfAbsent(clazz, c -> this.buildMeta(c, true));
    }

    private List<LazyFieldMeta> buildMeta(Class<?> clazz, boolean staticFields) {
        List<LazyFieldMeta> metas = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                LazySync annotation = field.getAnnotation(LazySync.class);
                if (annotation == null) continue;
                if (Modifier.isStatic(field.getModifiers()) != staticFields) continue;
                StreamCodec<? extends ByteBuf, ?> rawCodec = SyncProxy.defaultCodec(Util.cast(field.getType()));
                if (rawCodec == null) {
                    log.warn(
                        "No default codec for @LazySync field {}.{} of type {}, skipping",
                        c.getName(), field.getName(), field.getType().getName()
                    );
                    continue;
                }
                StreamCodec<? extends ByteBuf, Object> codec = Util.cast(rawCodec);
                field.setAccessible(true);
                metas.add(new LazyFieldMeta(field, codec, annotation.value(), "%s#%s".formatted(c.getName(), field.getName())));
            }
        }
        return Collections.unmodifiableList(metas);
    }

    public void clearServer() {
        this.server.instances.clear();
        this.server.statics.clear();
        this.server.snapshots.clear();
    }

    public void clearClient() {
        this.client.instances.clear();
        this.client.statics.clear();
        this.client.snapshots.clear();
    }
}
