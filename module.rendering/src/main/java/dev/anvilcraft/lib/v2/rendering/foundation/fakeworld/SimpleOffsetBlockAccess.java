package dev.anvilcraft.lib.v2.rendering.foundation.fakeworld;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SimpleOffsetBlockAccess extends SimpleDelegatingTintAccess {
    private final BlockPos offset;

    public SimpleOffsetBlockAccess(BlockAndTintGetter level, BlockPos offset) {
        super(level);
        this.offset = offset;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return super.getBlockState(pos.offset(offset));
    }
}
