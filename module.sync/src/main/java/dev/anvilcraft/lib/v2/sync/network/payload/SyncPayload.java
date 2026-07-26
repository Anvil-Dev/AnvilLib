package dev.anvilcraft.lib.v2.sync.network.payload;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import dev.anvilcraft.lib.v2.sync.client.AnvilLibSyncClient;
import dev.anvilcraft.lib.v2.sync.management.SyncProxy;
import dev.anvilcraft.lib.v2.sync.management.SyncRegisterEntry;
import dev.anvilcraft.lib.v2.sync.util.SideUtil;
import dev.anvilcraft.lib.v2.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Field;
import java.util.Objects;

@org.jetbrains.annotations.ApiStatus.Internal
public record SyncPayload(
    byte[] array
) implements IInsensitiveBiPacket {
    public static final Type<SyncPayload> TYPE = IPacket.type(AnvilLibSync.of("sync"));
    public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE_ARRAY,
        SyncPayload::array,
        SyncPayload::new
    );

    public static <T, ID> SyncPayload create(
        T parent,
        StreamCodec<? extends ByteBuf, ID> idCodec,
        ID id,
        SyncProxy<?> field,
        PacketFlow flow
    ) {
        FriendlyByteBuf buf = SideUtil.createFriendlyByteBuf(Unpooled.buffer());
        String syncConfig = "%s#%s".formatted(
            parent.getClass().getName(), // ClassName
            Objects.requireNonNull(field.getFieldName()) // FieldName
        );
        int syncConfigId = flow == PacketFlow.CLIENTBOUND
                               ? AnvilLibSync.SYNC_CONFIG_MANAGER.getId(syncConfig)
                               : AnvilLibSyncClient.SYNC_CONFIG_MANAGER.getId(syncConfig);
        buf.writeVarInt(syncConfigId); // SyncConfigId
        Util.<StreamCodec<FriendlyByteBuf, ID>>cast(idCodec).encode(buf, id); // ObjectId
        field.encode(buf); // FieldValue
        byte[] array = new byte[buf.readableBytes()];
        buf.readBytes(array);
        return new SyncPayload(array);
    }

    @SneakyThrows
    private void handler(IPayloadContext ctx) {
        FriendlyByteBuf buf = SideUtil.createFriendlyByteBuf(Unpooled.buffer());
        buf.writeBytes(this.array());
        int syncConfigId = buf.readVarInt();
        String syncConfig = SideUtil.isServer()
                            ? AnvilLibSync.SYNC_CONFIG_MANAGER.getById(syncConfigId)
                            : AnvilLibSyncClient.SYNC_CONFIG_MANAGER.getById(syncConfigId);
        String[] syncConfigSplit = syncConfig.split("#");
        String className = syncConfigSplit[0]; // ClassName
        String fieldName = syncConfigSplit[1]; // FieldName
        Class<?> clazz = Class.forName(className);
        SyncRegisterEntry<?, ?> entry = Objects.requireNonNull(
            AnvilLibSync.SYNC_MANAGER.contains(clazz),
            "Class " + className + " is not registered for syncing"
        );
        Object id = entry.idCodec().decode(buf); // ObjectId
        Object object = entry.finder().apply(ctx, Util.cast(id));
        boolean isStatic = object instanceof Class<?>;
        if (isStatic) clazz = (Class<?>) object;
        Field field = (clazz).getField(fieldName);
        SyncProxy<?> syncProxy = Util.cast(isStatic ? field.get(null) : field.get(object));
        syncProxy.setValue(buf, PacketFlow.SERVERBOUND.equals(ctx.flow())); // FieldValue
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
        return SyncPayload.TYPE;
    }
}
