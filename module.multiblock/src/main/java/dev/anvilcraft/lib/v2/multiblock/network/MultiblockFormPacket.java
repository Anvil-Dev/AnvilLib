package dev.anvilcraft.lib.v2.multiblock.network;

import dev.anvilcraft.lib.v2.multiblock.AnvilLibMultiblock;
import dev.anvilcraft.lib.v2.multiblock.dynamic.DynamicMultiblockManager;
import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.ControllerRecord;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.multiblock.dynamic.event.DynamicMultiblockEvent;
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
import net.neoforged.neoforge.common.NeoForge;

@Slf4j
@SuppressWarnings("unused")
public record MultiblockFormPacket(MultiblockState state) implements IClientboundPacket {
    public static final Type<MultiblockFormPacket> TYPE = IPacket.type(AnvilLibMultiblock.of("multiblock_form"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockFormPacket> STREAM_CODEC = StreamCodec.composite(
        MultiblockState.STREAM_CODEC,
        MultiblockFormPacket::state,
        MultiblockFormPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return MultiblockFormPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        Level level = player.level();
        DynamicMultiblockManager manager = DynamicMultiblockManager.get(level);
        manager.add(this.state);
        manager.updateFormed(level, this.state, true);
        BlockPos pos = this.state.getControllerPos();
        BlockState state = level.getBlockState(pos);
        if (!this.state.getDefinition(level.registryAccess()).value().isController(level, state, level.getBlockEntity(pos))) return;
        try {
            IController controller = ControllerRecord.get(state.getBlock(), this.state.getDefinitionKey().identifier());
            DynamicMultiblockEvent.Form event = new DynamicMultiblockEvent.Form(level, controller, this.state);
            NeoForge.EVENT_BUS.post(event);
            if (!event.isCanceled()) {
                controller.onFormed(level, this.state);
            } else {
                this.state.setFormed(false);
            }
        } catch (IllegalArgumentException e) {
            log.error(e.getLocalizedMessage(), e);
            throw e;
        }
    }
}
