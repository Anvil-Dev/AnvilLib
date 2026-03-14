package dev.anvilcraft.lib.v2.multiblock.controller;

import net.minecraft.core.BlockPos;

public record MultiblockControllerInstance(IMultiblockController controller, BlockPos pos) {
}
