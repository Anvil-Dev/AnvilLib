package dev.anvilcraft.lib.v2.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public record BlockInfo(BlockPos pos, BlockState state, @Nullable BlockEntity entity) {
    public BlockInfo(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }
}
