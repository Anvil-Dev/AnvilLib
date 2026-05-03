package dev.anvilcraft.lib.v2.multiblock.network;

import dev.anvilcraft.lib.v2.multiblock.AnvilLibMultiblock;
import dev.anvilcraft.lib.v2.multiblock.dynamic.DynamicMultiblockManager;
import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.ControllerRecord;
import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Slf4j
@SuppressWarnings("unused")
public record MultiblockUnformPacket(MultiblockState state) implements IClientboundPacket {
    public static final Type<MultiblockUnformPacket> TYPE = IPacket.type(AnvilLibMultiblock.of("multiblock_unform"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockUnformPacket> STREAM_CODEC = StreamCodec.composite(
        MultiblockState.STREAM_CODEC,
        MultiblockUnformPacket::state,
        MultiblockUnformPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return MultiblockUnformPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        Level level = player.level();
        DynamicMultiblockManager manager = DynamicMultiblockManager.get(level);
        manager.add(this.state);
        manager.updateFormed(level, this.state, false);
        BlockPos pos = this.state.getControllerPos();
        BlockState state = level.getBlockState(pos);
        if (!this.state.getDefinition().value().isController(level, state, level.getBlockEntity(pos))) return;
        try {
            ControllerRecord.get(state.getBlock(), this.state.getDefinitionKey().identifier()).onUnformed(level, this.state);
        } catch (IllegalArgumentException e) {
            log.error(e.getLocalizedMessage(), e);
            throw e;
        }
    }
}
