package dev.anvilcraft.lib.v2.piston.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.anvilcraft.lib.v2.piston.AnvilLibMoveableEntityBlock;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.piston.injection.IPistonMovingBlockEntityExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Debug(export = true)
@Mixin(value = PistonBaseBlock.class, priority = 943)
abstract class PistonBaseBlockMixin {
    @WrapOperation(
        method = "isPushable",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z")
    )
    private static boolean isPushable(BlockState instance, Operation<Boolean> original) {
        return original.call(instance) && !(instance.getBlock() instanceof IMoveableEntityBlock);
    }

    @WrapOperation(
        method = "moveBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock("
                     + "Lnet/minecraft/core/BlockPos;"
                     + "Lnet/minecraft/world/level/block/state/BlockState;"
                     + "I"
                     + ")Z",
            ordinal = 1
        )
    )
    private boolean setBlock(
        Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original,
        @Local(ordinal = 1) Direction pushDirection,
        @Share(value = "sharedBlockEntity", namespace = AnvilLibMoveableEntityBlock.MAIN_ID) LocalRef<BlockEntity> sharedBlockEntity
    ) {
        sharedBlockEntity.set(null);
        BlockPos relative = pos.relative(pushDirection.getOpposite());
        if (
            level.getBlockState(relative).getBlock() instanceof IMoveableEntityBlock
            && level.getBlockEntity(relative) instanceof BlockEntity blockEntity
        ) {
            sharedBlockEntity.set(blockEntity);
            level.removeBlockEntity(relative);
        }
        return original.call(level, pos, state, flags);
    }

    @WrapOperation(
        method = "moveBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/MovingPistonBlock;newMovingBlockEntity("
                     + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;"
                     + "Lnet/minecraft/world/level/block/state/BlockState;"
                     + "Lnet/minecraft/core/Direction;ZZ"
                     + ")Lnet/minecraft/world/level/block/entity/BlockEntity;",
            ordinal = 0
        )
    )
    private BlockEntity newMovingBlockEntity(
        BlockPos position,
        BlockState blockState,
        BlockState movedState,
        Direction direction,
        boolean extending,
        boolean isSourcePiston,
        Operation<BlockEntity> original,
        @Share(value = "sharedBlockEntity", namespace = AnvilLibMoveableEntityBlock.MAIN_ID) LocalRef<BlockEntity> sharedBlockEntity
    ) {
        BlockEntity blockEntity = original.call(position, blockState, movedState, direction, extending, isSourcePiston);
        if (blockEntity instanceof IPistonMovingBlockEntityExtension entity) {
            entity.anvillib$setBlockEntity(sharedBlockEntity.get());
        }
        return blockEntity;
    }
}
