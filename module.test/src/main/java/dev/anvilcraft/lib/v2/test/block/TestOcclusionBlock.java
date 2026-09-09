package dev.anvilcraft.lib.v2.test.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.test.all.TestTiles;
import dev.anvilcraft.lib.v2.test.block.tile.TestOcclusionTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TestOcclusionBlock extends BaseEntityBlock {
    public TestOcclusionBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(TestOcclusionBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TestOcclusionTile(TestTiles.TEST_OCCLUSION.get(),blockPos, blockState);
    }
}

