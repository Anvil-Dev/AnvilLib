package dev.anvilcraft.lib.v2.cube.mixin.client;

import dev.anvilcraft.lib.v2.cube.client.CubePicking;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockGetter.class)
interface BlockGetterMixin {
    @Inject(method = "clipWithInteractionOverride", at = @At("HEAD"), cancellable = true)
    private void anvillib_cube$clip(Vec3 start, Vec3 end, BlockPos pos, VoxelShape shape, BlockState state,
                                    CallbackInfoReturnable<BlockHitResult> callback) {
        CubeSelection.Target target = CubePicking.target((BlockGetter) this, pos, state);
        if (target == null) return;
        BlockHitResult hit = target.clip(pos, start, end);
        // 超出当前遍历格的命中留给邻格补扫，否则会提前遮挡下一格中更近的普通方块。
        if (hit != null) {
            Vec3 point = hit.getLocation();
            if (point.x < pos.getX() - 1.0E-7 || point.x > pos.getX() + 1 + 1.0E-7
                || point.y < pos.getY() - 1.0E-7 || point.y > pos.getY() + 1 + 1.0E-7
                || point.z < pos.getZ() - 1.0E-7 || point.z > pos.getZ() + 1 + 1.0E-7) hit = null;
        }
        callback.setReturnValue(hit);
    }
}
