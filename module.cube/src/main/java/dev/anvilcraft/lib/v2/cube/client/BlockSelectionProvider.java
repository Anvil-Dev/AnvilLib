package dev.anvilcraft.lib.v2.cube.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** 可选的 BER 适配器；返回使用同一插值时刻和同一变换的部件，不读取渲染器内部状态。 */
@FunctionalInterface
public interface BlockSelectionProvider {
    List<SelectionPart> parts(ClientLevel level, BlockPos pos, BlockState state, float partialTick);
}
