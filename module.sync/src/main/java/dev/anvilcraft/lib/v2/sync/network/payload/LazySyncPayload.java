package dev.anvilcraft.lib.v2.sync.network.payload;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import dev.anvilcraft.lib.v2.sync.client.AnvilLibSyncClient;
import dev.anvilcraft.lib.v2.sync.management.LazySyncManager;
import dev.anvilcraft.lib.v2.sync.management.SyncRegisterEntry;
import dev.anvilcraft.lib.v2.sync.util.SideUtil;
import dev.anvilcraft.lib.v2.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 惰性同步网络包。
 *
 * <p>每个网络包对应「一个对象」在某 tick 内发生变更的「全部字段」，从而实现按对象分组、
 * 每对象每 tick 仅一个网络包。与 {@link SyncPayload} 逐字段发送不同。</p>
 *
 * <p>负载布局（写入单个 {@code byte[]}）：</p>
 * <pre>
 * String  lookupClassName   // 解析 SyncRegisterEntry 所用的运行时类名（静态字段为 java.lang.Class）
 * (idCodec) objectId        // 对象 id，由对应 entry 的 idCodec 编解码
 * VarInt  fieldCount        // 变更字段数量
 * repeat:
 *   VarInt syncConfigId     // 字段配置 id（ClassName#fieldName 的压缩 id）
 *   VarInt blobLength
 *   byte[] blob             // 字段值负载：boolean(isNull) + (codec 编码值)
 * </pre>
 */
@Slf4j
@ApiStatus.Internal
public record LazySyncPayload(
    byte[] array
) implements IInsensitiveBiPacket {
    public static final Type<LazySyncPayload> TYPE = IPacket.type(AnvilLibSync.of("lazy_sync"));
    public static final StreamCodec<ByteBuf, LazySyncPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE_ARRAY,
        LazySyncPayload::array,
        LazySyncPayload::new
    );

    /**
     * 一个变更字段。
     *
     * @param configKey 字段的配置键，形如 {@code DeclaringClassName#fieldName}
     * @param blob      字段值负载（boolean(isNull) + codec 编码值）
     */
    public record FieldChange(String configKey, byte[] blob) {
    }

    public static <ID> LazySyncPayload create(
        StreamCodec<? extends ByteBuf, ID> idCodec,
        ID id,
        String lookupClassName,
        List<FieldChange> changes,
        PacketFlow flow
    ) {
        FriendlyByteBuf buf = SideUtil.createFriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(lookupClassName);
        Util.<StreamCodec<FriendlyByteBuf, ID>>cast(idCodec).encode(buf, id);
        buf.writeVarInt(changes.size());
        for (FieldChange change : changes) {
            int syncConfigId = flow == PacketFlow.CLIENTBOUND
                                   ? AnvilLibSync.SYNC_CONFIG_MANAGER.getId(change.configKey())
                                   : AnvilLibSyncClient.SYNC_CONFIG_MANAGER.getId(change.configKey());
            buf.writeVarInt(syncConfigId);
            buf.writeVarInt(change.blob().length);
            buf.writeBytes(change.blob());
        }
        byte[] array = new byte[buf.readableBytes()];
        buf.readBytes(array);
        return new LazySyncPayload(array);
    }

    private void handler(IPayloadContext ctx) {
        FriendlyByteBuf buf = SideUtil.createFriendlyByteBuf(Unpooled.buffer());
        buf.writeBytes(this.array());

        Object object;
        List<LazySyncManager.AppliedField> applied;
        try {
            String lookupClassName = buf.readUtf();
            Class<?> lookupClass = Class.forName(lookupClassName);
            SyncRegisterEntry<?, ?> entry = AnvilLibSync.SYNC_MANAGER.contains(lookupClass);
            if (entry == null) {
                // 两端注册不一致时静默丢弃，避免抛异常传播到 Netty 导致连接断开
                log.warn("Received LazySync packet for unregistered class {}, dropping", lookupClassName);
                return;
            }
            Object id = entry.idCodec().decode(buf);
            object = entry.finder().apply(ctx, Util.cast(id));

            int fieldCount = buf.readVarInt();
            boolean isServer = SideUtil.isServer();
            applied = new ArrayList<>(fieldCount);
            for (int i = 0; i < fieldCount; i++) {
                int syncConfigId = buf.readVarInt();
                int blobLength = buf.readVarInt();
                byte[] blob = new byte[blobLength];
                buf.readBytes(blob);
                String configKey = isServer
                                       ? AnvilLibSync.SYNC_CONFIG_MANAGER.getById(syncConfigId)
                                       : AnvilLibSyncClient.SYNC_CONFIG_MANAGER.getById(syncConfigId);
                applied.add(new LazySyncManager.AppliedField(configKey, blob));
            }
        } catch (Exception e) {
            log.warn("Failed to parse LazySync packet, dropping", e);
            return;
        }
        if (object == null) return;
        AnvilLibSync.LAZY_SYNC_MANAGER.applyGrouped(object, applied);
    }

    @Override
    public void bidirectionalHandler(IPayloadContext ctx) {
        IInsensitiveBiPacket.super.bidirectionalHandler(ctx);
        ctx.enqueueWork(() -> this.handler(ctx));
    }

    @Override
    public void handleOnBothSide(Player player) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return LazySyncPayload.TYPE;
    }
}
