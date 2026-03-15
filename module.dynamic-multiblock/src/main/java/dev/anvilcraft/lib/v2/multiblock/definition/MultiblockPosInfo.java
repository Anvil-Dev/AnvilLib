package dev.anvilcraft.lib.v2.multiblock.definition;

import net.minecraft.core.BlockPos;

public record MultiblockPosInfo(char key, BlockPos offset, BlockPos pos) {
}
