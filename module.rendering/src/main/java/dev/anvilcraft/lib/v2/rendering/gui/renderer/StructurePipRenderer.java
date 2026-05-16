package dev.anvilcraft.lib.v2.rendering.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.TransformingVertexConsumerWrapper;
import dev.anvilcraft.lib.v2.rendering.foundation.fakeworld.SimpleTintedEmptyLevelAccess;
import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import dev.anvilcraft.lib.v2.rendering.util.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;


public class StructurePipRenderer extends PictureInPictureRenderer<StructurePipRenderingState> {

    private final BlockAndTintGetter emptyTintedLevel = new SimpleTintedEmptyLevelAccess();

    public StructurePipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<StructurePipRenderingState> getRenderStateClass() {
        return StructurePipRenderingState.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void renderToTexture(StructurePipRenderingState renderState, PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        int guiScale = minecraft.gameRenderer.getGameRenderState().windowRenderState.guiScale;
        int width = (renderState.x1() - renderState.x0()) * guiScale;
        int height = (renderState.y1() - renderState.y0()) * guiScale;
        float scale = guiScale * renderState.scale();
        BlockAndTintGetter level = renderState.structureAccess();

        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(renderState.ambientOcclusion(), true, minecraft.getBlockColors());
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
                        VertexConsumer buffer = this.bufferSource.getBuffer(quad.materialInfo().itemRenderType());
                        buffer.putBakedQuad(poseStack.last(), quad, instance);
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
        bufferSource.endBatch();
        Map<ChunkSectionLayer, VertexConsumer> bufferBuilderMap = new HashMap<>();
        poseStack.pushPose();
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        for (BlockPos blockPos : BlockPos.betweenClosed(renderState.startPos(), renderState.endPos())) {
            BlockState blockState = level.getBlockState(blockPos);
            FluidState fluidState = level.getFluidState(blockPos);
            if (!fluidState.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                fluidRenderer.tesselate(
                    emptyTintedLevel,
                    BlockPos.ZERO,
                    layer -> {
                        VertexConsumer bufferBuilder = bufferBuilderMap.get(layer);
                        if (bufferBuilder != null) {
                            return bufferBuilder;
                        }
                        VertexConsumer consumer = createChunkSectionLayer(layer, poseStack);
                        bufferBuilderMap.put(layer, consumer);
                        return consumer;
                    },
                    blockState,
                    fluidState
                );
                poseStack.popPose();
            }
        }

        bufferSource.endBatch();
        for (BlockPos blockPos : BlockPos.betweenClosed(renderState.startPos(), renderState.endPos())) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity == null) continue;
            BlockEntityRenderer blockEntityRenderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            if (blockEntityRenderer == null) continue;

            RenderBuffers renderBuffers = minecraft.renderBuffers();
            SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
            GameRenderer gameRenderer = minecraft.gameRenderer;
            FeatureRenderDispatcher frd = new FeatureRenderDispatcher(
                submitNodeStorage,
                minecraft.getModelManager(),
                renderBuffers.bufferSource(),
                minecraft.getAtlasManager(),
                renderBuffers.outlineBufferSource(),
                renderBuffers.crumblingBufferSource(),
                minecraft.font,
                gameRenderer.getGameRenderState()
            );
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
                submitNodeStorage,
                gameRenderer.getGameRenderState().levelRenderState.cameraRenderState
            );
            frd.renderAllFeatures();
            poseStack.popPose();
        }

        poseStack.popPose();
        poseStack.popPose();
    }

    public VertexConsumer createChunkSectionLayer(ChunkSectionLayer layer, PoseStack poseStack) {
        RenderType renderType = switch (layer) {
            case SOLID, CUTOUT -> Sheets.cutoutBlockSheet();
            case TRANSLUCENT -> Sheets.translucentBlockSheet();
        };
        return new TransformingVertexConsumerWrapper(poseStack.last(), this.bufferSource.getBuffer(renderType));
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
