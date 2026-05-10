package dev.anvilcraft.lib.v2.test.block;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.test.all.TestTiles;
import dev.anvilcraft.lib.v2.test.block.tile.TestBloomTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestBloomBlock extends BaseEntityBlock {
    public TestBloomBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(TestBloomBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TestBloomTile(TestTiles.TEST_BLOOM.get(),blockPos, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof TestBloomTile tile) {
            if (player.isShiftKeyDown() && level.isClientSide()) {
                tile.aaa.setValue(tile.aaa.getValue() + 1);
            } else if (!player.isShiftKeyDown() && !level.isClientSide()) {
                tile.aaa.setValue(tile.aaa.getValue() - 1);
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
