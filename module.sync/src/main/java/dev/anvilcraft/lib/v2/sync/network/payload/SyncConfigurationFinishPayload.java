package dev.anvilcraft.lib.v2.sync.network.payload;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SyncConfigurationFinishPayload implements IServerboundPacket {
    public static final SyncConfigurationFinishPayload INSTANCE = new SyncConfigurationFinishPayload();
    public static final Type<SyncConfigurationFinishPayload> TYPE = IPacket.type(AnvilLibSync.of("sync_config_finish"));
    public static final StreamCodec<ByteBuf, SyncConfigurationFinishPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public void handleOnServer(Player player) {
    }

    @Override
    public void serverHandler(IPayloadContext ctx) {
        ctx.finishCurrentTask(AnvilLibSync.SyncConfig.TYPE);
    }

    @Override
    public Type<SyncConfigurationFinishPayload> type() {
        return SyncConfigurationFinishPayload.TYPE;
    }
}
