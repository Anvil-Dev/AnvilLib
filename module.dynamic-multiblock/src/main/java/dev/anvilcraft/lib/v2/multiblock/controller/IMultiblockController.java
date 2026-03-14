package dev.anvilcraft.lib.v2.multiblock.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface IMultiblockController {
    Block block();

    static IMultiblockController of(Block block) {
        if (block instanceof IMultiblockController controller) return controller;
        return new SimpleMultiblockController(block);
    }

    void onStructureValid(Level level, BlockPos pos, BlockState state);

    void onStructureInvalid(Level level, BlockPos pos, BlockState state);
}
