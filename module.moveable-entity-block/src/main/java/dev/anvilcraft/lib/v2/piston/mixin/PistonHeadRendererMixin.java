package dev.anvilcraft.lib.v2.piston.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonHeadRenderer.class)
abstract class PistonHeadRendererMixin implements BlockEntityRenderer<PistonMovingBlockEntity, PistonHeadRenderState> {
    @Inject(
        method = "extractRenderState("
                 + "Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;"
                 + "Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;"
                 + "FLnet/minecraft/world/phys/Vec3;"
                 + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;"
                 + ")V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/PistonHeadRenderer;createMovingBlock("
                     + "Lnet/minecraft/core/BlockPos;"
                     + "Lnet/minecraft/world/level/block/state/BlockState;"
                     + "Lnet/minecraft/core/Holder;"
                     + "Lnet/minecraft/client/multiplayer/ClientLevel;"
                     + ")Lnet/minecraft/client/renderer/block/MovingBlockRenderState;"
        )
    )
    private void extractRenderState(
        PistonMovingBlockEntity blockEntity,
        PistonHeadRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.CrumblingOverlay breakProgress,
        CallbackInfo ci
    ) {
        BlockEntity blockEntity1 = blockEntity.anvillib$getBlockEntity();
        if (blockEntity1 == null) return;
        BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = Minecraft.getInstance()
            .getBlockEntityRenderDispatcher()
            .getRenderer(blockEntity1);
        if (renderer == null) return;
        BlockEntityRenderState renderState = renderer.createRenderState();
        renderer.extractRenderState(blockEntity1, renderState, partialTicks, cameraPosition, breakProgress);
        state.anvillib$setExtraState(renderState);
    }

    @Inject(
        method = "submit("
                 + "Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;"
                 + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                 + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                 + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"
                 + ")V",
        at = @At("TAIL")
    )
    void submit(
        PistonHeadRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera,
        CallbackInfo ci
    ) {
        BlockEntityRenderState renderState = state.anvillib$getExtraState();
        if (renderState == null) return;
        poseStack.pushPose();
        poseStack.translate(state.xOffset, state.yOffset, state.zOffset);
        BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = Minecraft.getInstance()
            .getBlockEntityRenderDispatcher()
            .getRenderer(renderState);
        if (renderer != null) {
            renderer.submit(renderState, poseStack, submitNodeCollector, camera);
        }
        poseStack.popPose();
    }
}
