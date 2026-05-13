package dev.anvilcraft.lib.v2.test.block.tile;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestCachedRenderingTile extends BlockEntity {
    public TestCachedRenderingTile(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level.isClientSide()) {
            onClientRemove();
        }
    }

    private void onClientRemove() {
        CachedBlockEntityRenderingPipeline.getInstance().blockRemoved(this);
    }
}
