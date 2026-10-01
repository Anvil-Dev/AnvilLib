package dev.anvilcraft.lib.v2.piston.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.piston.injection.IPistonMovingBlockEntityExtension;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.mohistmc.youer.util.PaperUnsupportedSettings", remap = false)
abstract class YouerPistonCompatibilityMixin {
    @WrapOperation(
        method = "end_allowPistonDuplication",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
        )
    )
    private static void preserveBlockEntity(Level level, BlockEntity replacement, Operation<Void> original) {
        if (replacement instanceof PistonMovingBlockEntity moving
            && moving.getMovedState().getBlock() instanceof IMoveableEntityBlock
            && replacement instanceof IPistonMovingBlockEntityExtension replacementExtension
            && replacementExtension.anvillib$getBlockEntity() == null
            && level.getBlockEntity(replacement.getBlockPos()) instanceof IPistonMovingBlockEntityExtension existing) {
            BlockEntity carried = existing.anvillib$getBlockEntity();
            if (carried != null && carried.getType().isValid(moving.getMovedState())) {
                replacementExtension.anvillib$setBlockEntity(carried);
            }
        }
        original.call(level, replacement);
    }
}
