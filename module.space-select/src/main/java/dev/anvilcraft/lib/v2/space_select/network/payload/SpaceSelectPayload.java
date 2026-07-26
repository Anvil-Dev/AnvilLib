package dev.anvilcraft.lib.v2.space_select.network.payload;

import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.space_select.AnvilLibSpaceSelect;
import dev.anvilcraft.lib.v2.space_select.event.PlayerCreateDistrictEvent;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record SpaceSelectPayload(
    boolean offhand,
    BlockPos start,
    BlockPos end
) implements IServerboundPacket {
    public static final Type<SpaceSelectPayload> TYPE = new Type<>(AnvilLibSpaceSelect.of("space_select"));
    public static final StreamCodec<ByteBuf, SpaceSelectPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        SpaceSelectPayload::offhand,
        BlockPos.STREAM_CODEC,
        SpaceSelectPayload::start,
        BlockPos.STREAM_CODEC,
        SpaceSelectPayload::end,
        SpaceSelectPayload::new
    );

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        NeoForge.EVENT_BUS.post(new PlayerCreateDistrictEvent(
            serverPlayer,
            this.offhand() ? serverPlayer.getOffhandItem() : serverPlayer.getMainHandItem(),
            this.start(),
            this.end()
        ));
    }

    @Override
    public Type<SpaceSelectPayload> type() {
        return SpaceSelectPayload.TYPE;
    }
}
