package dev.anvilcraft.lib.v2.rendering.gui.renderer;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.TransformingVertexConsumerWrapper;
import dev.anvilcraft.lib.v2.rendering.foundation.fakeworld.SimpleTintedEmptyLevelAccess;
import dev.anvilcraft.lib.v2.rendering.glitch.GlitchPostEffect;
import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import dev.anvilcraft.lib.v2.rendering.util.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import dev.anvilcraft.lib.v2.rendering.foundation.FeatureGeometry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.EmptyVertexConsumer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;



@ApiStatus.Internal
public class StructurePipRenderer extends PictureInPictureRenderer<StructurePipRenderingState> {

    private final BlockAndTintGetter emptyTintedLevel = new SimpleTintedEmptyLevelAccess();


    @Override
    public Class<StructurePipRenderingState> getRenderStateClass() {
        return StructurePipRenderingState.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void renderToTexture(StructurePipRenderingState renderState, PoseStack poseStack, SubmitNodeCollector collector) {
        Minecraft minecraft = Minecraft.getInstance();
        int guiScale = minecraft.gameRenderer.gameRenderState().windowRenderState.guiScale;
        int width = (renderState.x1() - renderState.x0()) * guiScale;
        int height = (renderState.y1() - renderState.y0()) * guiScale;
        float scale = guiScale * renderState.scale();
        BlockAndTintGetter level = renderState.structureAccess();

        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(
            renderState.ambientOcclusion(),
            true,
            minecraft.getBlockColors()
        );
        FluidRenderer fluidRenderer = new FluidRenderer(minecraft.getModelManager().getFluidStateModelSet());

        poseStack.pushPose();
        poseStack.setIdentity();

        poseStack.translate(width / 2f, height / 2f, 0);
        poseStack.scale(1, -1, 1);
        poseStack.translate(-width / 2f, -height / 2f, 0);

        poseStack.translate(width / 2f, height / 2f, 0);

        poseStack.scale(scale, scale, scale);

        poseStack.last().pose().mul(renderState.pose3D().pose());
        poseStack.last().normal().mul(renderState.pose3D().normal());

        for (BlockPos blockPos : BlockPos.betweenClosed(renderState.startPos(), renderState.endPos())) {
            BlockState blockState = level.getBlockState(blockPos);

            if (!blockState.isAir()) {
                blockRenderer.tesselateBlock(
                    (x, y, z, quad, instance) -> {
                        poseStack.pushPose();
                        poseStack.translate(x, y, z);
                        poseStack.translate(-0.5f, -0.5f, -0.5f);
                        FeatureGeometry.submitQuad(collector, poseStack, quad, instance);
                        poseStack.popPose();
                    },
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ(),
                    level,
                    blockPos,
                    blockState,
                    minecraft.getModelManager().getBlockStateModelSet().get(blockState),
                    blockState.getSeed(blockPos)
                );
            }
        }
        poseStack.pushPose();
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        for (BlockPos blockPos : BlockPos.betweenClosed(renderState.startPos(), renderState.endPos())) {
            BlockState blockState = level.getBlockState(blockPos);
            FluidState fluidState = level.getFluidState(blockPos);
            if (!fluidState.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                for (ChunkSectionLayer targetLayer : ChunkSectionLayer.values()) {
                    collector.submitCustomGeometry(poseStack, renderTypeForLayer(targetLayer), (pose, buffer) ->
                        fluidRenderer.tesselate(emptyTintedLevel, BlockPos.ZERO,
                            layer -> layer == targetLayer ? new TransformingVertexConsumerWrapper(pose, buffer) : EmptyVertexConsumer.INSTANCE,
                            blockState, fluidState));
                }
                poseStack.popPose();
            }
        }


        GameRenderer gameRenderer = minecraft.gameRenderer;
        for (BlockPos blockPos : BlockPos.betweenClosed(renderState.startPos(), renderState.endPos())) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity == null) continue;
            BlockEntityRenderer blockEntityRenderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            if (blockEntityRenderer == null) continue;
            poseStack.pushPose();
            poseStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            BlockEntityRenderState blockEntityRenderState = blockEntityRenderer.createRenderState();
            blockEntityRenderer.extractRenderState(
                blockEntity,
                blockEntityRenderState,
                Timer.getPartialTick(),
                Vec3.ZERO,
                null
            );
            blockEntityRenderer.submit(
                blockEntityRenderState,
                poseStack,
                collector,
                gameRenderer.gameRenderState().levelRenderState.cameraRenderState
            );
            poseStack.popPose();
        }
        if (renderState.drawAdditionalCallback() != null) {
            renderState.drawAdditionalCallback().accept(collector, poseStack);
        }

        poseStack.popPose();
        poseStack.popPose();
        if (!poseStack.isEmpty()) {
            throw new IllegalStateException("Pose stack not empty");
        }
    }

    @Override
    protected void blitTexture(StructurePipRenderingState state, GuiRenderState guiState) {
        if (!state.glitched()) {
            super.blitTexture(state, guiState);
            return;
        }
        int width = this.textureView.getWidth(0);
        int height = this.textureView.getHeight(0);
        GlitchPostEffect effect = ALRPostEffects.getGlitchPostEffect();
        GpuTextureView processed = effect.process(this.textureView, width, height);
        guiState.addBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(processed, effect.getSampler()), state.pose(),
            state.x0(), state.y0(), state.x1(), state.y1(), 0,
            width / (float) effect.getGlitchOutputTarget().width,
            height / (float) effect.getGlitchOutputTarget().height, 0, -1, state.scissorArea(), null));
    }

    private static RenderType renderTypeForLayer(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID, CUTOUT -> Sheets.cutoutBlockItemSheet();
            case TRANSLUCENT -> Sheets.translucentBlockItemSheet();
        };
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 4f;
    }

    @Override
    protected String getTextureLabel() {
        return "structure";
    }
}
