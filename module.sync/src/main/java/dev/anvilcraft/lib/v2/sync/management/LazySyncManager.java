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
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/**
 * 惰性同步管理器。
 *
 * <p>负责被 {@link LazySync} 标注字段的「扫描 - 差分 - 分组同步」：</p>
 * <ul>
 *   <li>实例 / 静态目标由字节码注入在 {@code <init>} / {@code <clinit>} 中登记
 *       （见 {@link dev.anvilcraft.lib.v2.sync.transform.LazySyncBytecodeInjector
 *       LazySyncBytecodeInjector}）。</li>
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
