package dev.anvilcraft.lib.v2.rendering.gui.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.TransformingVertexConsumerWrapper;
import dev.anvilcraft.lib.v2.rendering.foundation.fakeworld.SimpleTintedEmptyLevelAccess;
import dev.anvilcraft.lib.v2.rendering.glitch.GlitchPostEffect;
import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import dev.anvilcraft.lib.v2.rendering.util.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderPipelines;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;


@org.jetbrains.annotations.ApiStatus.Internal
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
                submitNodeStorage,
                gameRenderer.getGameRenderState().levelRenderState.cameraRenderState
            );
            poseStack.popPose();
        }
        if (renderState.drawAdditionalCallback() != null) {
            renderState.drawAdditionalCallback().accept(submitNodeStorage, poseStack);
        }
        frd.renderAllFeatures();
        bufferSource.endBatch();

        poseStack.popPose();
        poseStack.popPose();
        if (!poseStack.isEmpty()) {
            throw new IllegalStateException("Pose stack not empty");
        }
        if (renderState.glitched()) {
            applyGlitchEffect(width, height);
        }
    }

    public void applyGlitchEffect(int width, int height) {
        GlitchPostEffect glitchPostEffect = ALRPostEffects.getGlitchPostEffect();
        GpuTextureView processed = glitchPostEffect.process(
            RenderSystem.outputColorTextureOverride,
            width,
            height
        );
        float u1 = width / (glitchPostEffect.getGlitchOutputTarget().width * 1.0f);
        float v1 = height / (glitchPostEffect.getGlitchOutputTarget().height * 1.0f);
        Tesselator tesselator = Tesselator.getInstance();
        VertexFormat format = DefaultVertexFormat.POSITION_TEX_COLOR;
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, format);

        builder.addVertex(0, 0, 100).setUv(0, 0).setColor(-1);
        builder.addVertex(0, height, 100).setUv(0, v1).setColor(-1);
        builder.addVertex(width, height, 100).setUv(u1, v1).setColor(-1);
        builder.addVertex(width, 0, 100).setUv(u1, 0).setColor(-1);

        MeshData data = builder.buildOrThrow();
        GpuBuffer buffer = format.uploadImmediateVertexBuffer(data.vertexBuffer());
        int indexCount = data.drawState().indexCount();
        data.close();
        RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = sequentialBuffer.getBuffer(indexCount);

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(
                new Matrix4f(),
                new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                new Vector3f(),
                new Matrix4f()
            );

        try (RenderPass renderPass = commandEncoder.createRenderPass(
            () -> "Immediate draw for blitGlitchEffect",
            RenderSystem.outputColorTextureOverride,
            OptionalInt.of(0)
        )) {
            renderPass.setPipeline(RenderPipelines.GUI_TEXTURED);
            renderPass.bindTexture("Sampler0", processed, glitchPostEffect.getSampler());
            renderPass.setVertexBuffer(0, buffer);
            renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.type());
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.drawIndexed(0, 0, indexCount, 1);
        }
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
