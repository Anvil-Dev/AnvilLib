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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

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

    @Inject(
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
    private void setBlock(
        Level level, BlockPos pistonPos, Direction direction, boolean extending, CallbackInfoReturnable<Boolean> cir,
        @Local(name = "pos") BlockPos pos,
        @Local(name = "pushDirection") Direction pushDirection,
        @Local(name = "toPushShapes") List<BlockState> toPushShapes,
        @Local(name = "i") int i,
        @Share(value = "sharedBlockEntity", namespace = AnvilLibMoveableEntityBlock.MAIN_ID) LocalRef<BlockEntity> sharedBlockEntity
    ) {
        BlockPos relative = pos.relative(pushDirection.getOpposite());
        if (
            toPushShapes.get(i).getBlock() instanceof IMoveableEntityBlock
            && level.getBlockEntity(relative) instanceof BlockEntity blockEntity
        ) {
            sharedBlockEntity.set(blockEntity);
            level.removeBlockEntity(relative);
        }
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
