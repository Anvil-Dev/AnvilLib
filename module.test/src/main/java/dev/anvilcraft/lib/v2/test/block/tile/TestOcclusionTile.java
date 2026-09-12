package dev.anvilcraft.lib.v2.test.block.tile;

import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionKey;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class TestOcclusionTile extends BlockEntity {

    @Getter
    private final OcclusionKey occlusionKey;

    public TestOcclusionTile(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
        this.occlusionKey = new OcclusionKey(
            () -> "TestOcclusionTile@" + worldPosition.toShortString(),
            new AABB(worldPosition)
        );
    }
}
