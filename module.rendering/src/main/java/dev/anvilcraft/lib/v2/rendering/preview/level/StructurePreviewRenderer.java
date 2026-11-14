package dev.anvilcraft.lib.v2.rendering.preview.level;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.preview.SectionOffsetVertexConsumer;
import dev.anvilcraft.lib.v2.rendering.preview.SodiumSpriteBridge;
import dev.anvilcraft.lib.v2.rendering.preview.ViewportCameraRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 将 {@link SandboxRenderLevel} 渲染到当前 GUI 渲染目标。
 *
 * <p>该渲染器在保持轻量单关卡预览管线的同时，尽量对齐原版世界渲染顺序，
 * 以减少渲染异常。</p>
 */
public class StructurePreviewRenderer {
    /** 调用方可在结构之后绘制模型及覆盖效果，不需要注入渲染器。 */
    public interface Effects {
        Effects NONE = new Effects() {};

        default void submit(MultiBufferSource.BufferSource buffers) {
        }

        default void render() {
        }
    }

    private static @Nullable StructurePreviewRenderer instance;

    private final PreviewLightmapAtlas lightmap = new PreviewLightmapAtlas();

    public static StructurePreviewRenderer getInstance() {
        RenderSystem.assertOnRenderThread();
        if (StructurePreviewRenderer.instance == null) {
            StructurePreviewRenderer.instance = new StructurePreviewRenderer();
        }
        return StructurePreviewRenderer.instance;
    }

    /**
     * 使用内部持有的缓冲源渲染关卡。
     */
    public void render(
        SandboxRenderLevel level,
        ViewportCameraRig cameraRig
    ) {
        lightmap.update(level);

        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT);

        level.tickFrameClock();

        RenderSystem.setShaderGameTime(level.getGameTime(), level.getPartialTick());

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        render(level, cameraRig, buffers);
        buffers.endBatch();
    }

    /**
     * 使用调用方提供的缓冲源渲染关卡。
     */
    public void render(
        SandboxRenderLevel level,
        ViewportCameraRig cameraRig,
        MultiBufferSource.BufferSource buffers
    ) {
        this.render(level, cameraRig, buffers, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * 使用调用方提供的缓冲源渲染指定可见层范围内的关卡内容。
     */
    public void render(
        SandboxRenderLevel level,
        ViewportCameraRig cameraRig,
        MultiBufferSource.BufferSource buffers,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        this.render(level, cameraRig, buffers, visibleMinY, visibleMaxYExclusive, Effects.NONE);
    }

    /** 在预览相机矩阵有效期间绘制额外内容，随后恢复 GUI 绘制状态。 */
    public void render(SandboxRenderLevel level, ViewportCameraRig cameraRig, MultiBufferSource.BufferSource buffers,
                       int visibleMinY, int visibleMaxYExclusive, Effects effects) {
        java.util.Objects.requireNonNull(effects);
        lightmap.update(level);

        // 先清空待处理光照任务，确保本帧光照采样稳定。
        var lightEngine = level.getLightEngine();
        while (lightEngine.hasLightWork()) {
            lightEngine.runLightUpdates();
        }

        // 安装预览矩阵并关闭雾效，保证指南页面渲染结果稳定。
        var projectionMatrix = cameraRig.buildProjectionMatrix();
        var viewMatrix = cameraRig.buildViewMatrix();

        RenderSystem.setShaderFog(net.minecraft.client.renderer.FogParameters.NO_FOG);





        var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
        modelViewStack.mul(viewMatrix);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionMatrix, com.mojang.blaze3d.ProjectionType.ORTHOGRAPHIC);

        var lightDirection = new Vector4f(15 / 90f, .35f, 1, 0);
        var lightTransform = new Matrix4f(viewMatrix);
        lightTransform.invert();
        lightTransform.transform(lightDirection);

        Lighting.setupLevel();

        try {
            renderContent(level, buffers, visibleMinY, visibleMaxYExclusive, effects);
        // 自定义方块实体也可能使用上述标准渲染层以外的缓冲，必须在恢复 GUI 矩阵前提交。
        buffers.endBatch();

            effects.render();
        } finally {
        modelViewStack.popMatrix();
        RenderSystem.restoreProjectionMatrix();

        Lighting.setupFor3DItems();
        }
    }

    /**
     * 按接近原版关卡渲染的顺序执行各个绘制阶段。
     */
    public void renderContent(SandboxRenderLevel level, MultiBufferSource.BufferSource buffers) {
        this.renderContent(level, buffers, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * 按接近原版关卡渲染的顺序执行各个绘制阶段，并限制可见层范围。
     */
    public void renderContent(
        SandboxRenderLevel level,
        MultiBufferSource.BufferSource buffers,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        this.renderContent(level, buffers, visibleMinY, visibleMaxYExclusive, Effects.NONE);
    }

    private void renderContent(SandboxRenderLevel level, MultiBufferSource.BufferSource buffers,
                               int visibleMinY, int visibleMaxYExclusive, Effects effects) {
        {
            // 第一阶段：先绘制不透明内容与实体系统。
            renderBlocks(level, buffers, false, visibleMinY, visibleMaxYExclusive);
            renderBlockEntities(level, buffers, level.getPartialTick(), visibleMinY, visibleMaxYExclusive);
            renderEntities(level, buffers, level.getPartialTick(), visibleMinY, visibleMaxYExclusive);
            effects.submit(buffers);

            // 该顺序参考 LevelRenderer#renderLevel
            buffers.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
            buffers.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
            buffers.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
            buffers.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));

            // 这些层在原版通常已预烘焙，这里需要手动结束批次
            for (var layer : RenderType.chunkBufferLayers()) {
                if (layer != RenderType.translucent()) {
                    buffers.endBatch(layer);
                }
            }

            buffers.endBatch(RenderType.solid());
            buffers.endBatch(RenderType.endPortal());
            buffers.endBatch(RenderType.endGateway());
            buffers.endBatch(Sheets.solidBlockSheet());
            buffers.endBatch(Sheets.cutoutBlockSheet());
            buffers.endBatch(Sheets.bedSheet());
            buffers.endBatch(Sheets.shulkerBoxSheet());
            buffers.endBatch(Sheets.signSheet());
            buffers.endBatch(Sheets.hangingSignSheet());
            buffers.endBatch(Sheets.chestSheet());
            buffers.endLastBatch();

            // 第二阶段：在不透明缓冲全部提交后绘制半透明方块。
            renderBlocks(level, buffers, true, visibleMinY, visibleMaxYExclusive);
            buffers.endBatch(RenderType.translucent());
        }
    }

    private void renderBlocks(
        SandboxRenderLevel level,
        MultiBufferSource buffers,
        boolean translucent,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        var randomSource = level.random;
        var blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        var poseStack = new PoseStack();
        var layerView = new VisibleLayerBlockAndTintGetter(level, visibleMinY, visibleMaxYExclusive);

        level.getFilledBlocks().forEach(pos -> {
            if (!isVisibleLayer(pos.getY(), visibleMinY, visibleMaxYExclusive)) {
                return;
            }

            var blockState = level.getBlockState(pos);
            var fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty()) {
                var renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
                if ((renderType == RenderType.translucent()) == translucent) {
                    var bufferBuilder = buffers.getBuffer(renderType);

                    var sectionPos = SectionPos.of(pos);
                    var sectionOffsetWriter = new SectionOffsetVertexConsumer(bufferBuilder, sectionPos);
                    blockRenderDispatcher.renderLiquid(pos, layerView, sectionOffsetWriter, blockState, fluidState);

                    markFluidSpritesActive(fluidState);
                }
            }

            if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
                var be = level.getBlockEntity(pos);
                ModelData modelData = ModelData.EMPTY;
                if (be != null) {
                    modelData = be.getModelData();
                }

                var model = blockRenderDispatcher.getBlockModel(blockState);
                modelData = model.getModelData(layerView, pos, blockState, modelData);
                var renderTypes = model.getRenderTypes(blockState, randomSource, modelData);

                for (var renderType : renderTypes) {
                    if ((renderType == RenderType.translucent()) == translucent) {
                        var bufferBuilder = buffers.getBuffer(renderType);

                        poseStack.pushPose();
                        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                        blockRenderDispatcher.renderBatched(
                            blockState,
                            pos,
                            layerView,
                            poseStack,
                            bufferBuilder,
                            true,
                            randomSource,
                            modelData,
                            renderType
                        );
                        poseStack.popPose();
                    }
                }
            }
        });
    }

    private void renderBlockEntities(
        SandboxRenderLevel level,
        MultiBufferSource buffers,
        float partialTick,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        var poseStack = new PoseStack();

        level.getFilledBlocks().forEach(pos -> {
            if (!isVisibleLayer(pos.getY(), visibleMinY, visibleMaxYExclusive)) {
                return;
            }

            var blockState = level.getBlockState(pos);
            if (blockState.hasBlockEntity()) {
                var blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    this.handleBlockEntity(poseStack, blockEntity, buffers, partialTick);
                }
            }
        });
    }
    public static void markFluidSpritesActive(FluidState fluidState) {
        var props = IClientFluidTypeExtensions.of(fluidState);
        var sprite1 = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(props.getStillTexture());
        SodiumSpriteBridge.pingSpriteUsage(sprite1);
        var sprite2 = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(props.getFlowingTexture());
        SodiumSpriteBridge.pingSpriteUsage(sprite2);
    }

    private <E extends BlockEntity> void handleBlockEntity(PoseStack stack, E blockEntity, MultiBufferSource buffers, float partialTicks) {
        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(blockEntity);
        if (renderer != null && renderer.shouldRender(blockEntity, blockEntity.getBlockPos().getCenter())) {
            var pos = blockEntity.getBlockPos();
            stack.pushPose();
            stack.translate(pos.getX(), pos.getY(), pos.getZ());

            int packedLight = LevelRenderer.getLightColor(Objects.requireNonNull(blockEntity.getLevel()), blockEntity.getBlockPos());
            renderer.render(blockEntity, partialTicks, stack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
            stack.popPose();
        }
    }

    private void renderEntities(
        SandboxRenderLevel level,
        MultiBufferSource.BufferSource buffers,
        float partialTick,
        int visibleMinY,
        int visibleMaxYExclusive
    ) {
        var poseStack = new PoseStack();

        for (var entity : level.getEntitiesForRendering()) {
            if (entity.getBoundingBox().maxY > visibleMinY && entity.getBoundingBox().minY < visibleMaxYExclusive) {
                handleEntity(level, poseStack, entity, buffers, partialTick);
            }
        }
    }

    private static boolean isVisibleLayer(int y, int visibleMinY, int visibleMaxYExclusive) {
        return y >= visibleMinY && y < visibleMaxYExclusive;
    }

    private record VisibleLayerBlockAndTintGetter(
        SandboxRenderLevel delegate,
        int visibleMinY,
        int visibleMaxYExclusive
    ) implements BlockAndTintGetter {
        private boolean isVisible(BlockPos pos) {
            return isVisibleLayer(pos.getY(), this.visibleMinY, this.visibleMaxYExclusive);
        }

        @Override
        @Nullable
        public BlockEntity getBlockEntity(BlockPos pos) {
            return this.isVisible(pos) ? this.delegate.getBlockEntity(pos) : null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.isVisible(pos) ? this.delegate.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.isVisible(pos) ? this.delegate.getFluidState(pos) : Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return this.delegate.getShade(direction, shade);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return this.delegate.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
            return this.delegate.getBlockTint(blockPos, colorResolver);
        }

        @Override
        public int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
            return this.delegate.getBrightness(lightLayer, blockPos);
        }

        @Override
        public int getRawBrightness(BlockPos blockPos, int amount) {
            return this.delegate.getRawBrightness(blockPos, amount);
        }

        @Override
        public int getHeight() {
            return this.delegate.getHeight();
        }

        @Override
        public int getMinY() {
            return this.delegate.getMinY();
        }
    }

    private <E extends Entity> void handleEntity(
        SandboxRenderLevel level,
        PoseStack poseStack,
        E entity,
        MultiBufferSource buffers,
        float partialTicks
    ) {
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        this.handleEntity(level, poseStack, entity, buffers, partialTicks, dispatcher.getRenderer(entity));
    }

    private <E extends Entity, S extends net.minecraft.client.renderer.entity.state.EntityRenderState> void handleEntity(
        SandboxRenderLevel level, PoseStack poseStack, E entity, MultiBufferSource buffers, float partialTicks,
        net.minecraft.client.renderer.entity.EntityRenderer<? super E, S> renderer
    ) {

        var probePos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
        int packedLight = LevelRenderer.getLightColor(level, probePos);
        var yaw = entity.getYRot();

        var pos = entity.position();
        var renderState = renderer.createRenderState(entity, partialTicks);
        var offset = renderer.getRenderOffset(renderState);
        poseStack.pushPose();
        poseStack.translate(pos.x + offset.x(), pos.y + offset.y(), pos.z + offset.z());
        renderer.render(renderState, poseStack, buffers, packedLight);
        poseStack.popPose();
    }
}
