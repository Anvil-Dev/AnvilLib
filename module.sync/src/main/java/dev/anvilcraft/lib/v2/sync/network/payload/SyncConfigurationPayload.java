package dev.anvilcraft.lib.v2.sync.network.payload;

import org.jetbrains.annotations.ApiStatus;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import dev.anvilcraft.lib.v2.sync.client.AnvilLibSyncClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public record SyncConfigurationPayload(
    Map<Integer, String> syncMap
) implements IClientboundPacket {
    public static final StreamCodec<ByteBuf, SyncConfigurationPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.STRING_UTF8),
        SyncConfigurationPayload::syncMap,
        SyncConfigurationPayload::new
    );
    public static final Type<SyncConfigurationPayload> TYPE = IPacket.type(AnvilLibSync.of("sync_config"));

    @Override
    public void handleOnClient(Player player) {
    }

    @Override
    public void clientHandler(IPayloadContext ctx) {
        ctx.enqueueWork(() -> AnvilLibSyncClient.SYNC_CONFIG_MANAGER.registerAll(this.syncMap()));
        ctx.reply(SyncConfigurationFinishPayload.INSTANCE);
    }

    @Override
    public Type<SyncConfigurationPayload> type() {
        return SyncConfigurationPayload.TYPE;
    }
}
