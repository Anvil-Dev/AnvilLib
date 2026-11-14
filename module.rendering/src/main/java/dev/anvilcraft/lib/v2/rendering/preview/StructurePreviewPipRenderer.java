package dev.anvilcraft.lib.v2.rendering.preview;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.preview.level.SandboxRenderLevel;
import dev.anvilcraft.lib.v2.rendering.projection.ProjectionScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;

/** 使用隔离的原生 GUI 纹理目标，按可见层绘制完整结构。 */
public final class StructurePreviewPipRenderer extends PictureInPictureRenderer<StructurePreviewPipRenderer.State> {
    private final SubmitNodeStorage nodes = new SubmitNodeStorage();
    private final FeatureRenderDispatcher features;

    public StructurePreviewPipRenderer(MultiBufferSource.BufferSource buffers) {
        super(buffers);
        var client = Minecraft.getInstance();
        var renderBuffers = client.renderBuffers();
        this.features = new FeatureRenderDispatcher(this.nodes, client.getBlockRenderer(), buffers, client.getAtlasManager(),
            renderBuffers.outlineBufferSource(), renderBuffers.crumblingBufferSource(),
            client.font);
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "anvillib structure preview";
    }

    @Override
    public void close() {
        this.features.close();
        super.close();
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int guiScale = (int) mc.getWindow().getGuiScale();
        var view = new ProjectionScene(mc.level, BlockPos.ZERO);
        var blocks = state.level().getFilledBlocks()
            .filter(pos -> pos.getY() >= state.minY() && pos.getY() < state.maxY())
            .map(BlockPos::immutable).toList();
        var blockEntities = new ArrayList<BlockEntity>();
        var entities = new ArrayList<Entity>();
        for (var pos : blocks) {
            var entity = state.level().getBlockEntity(pos);
            view.put(pos, state.level().getBlockState(pos), entity);
            if (entity != null) {
                blockEntities.add(entity);
            }
        }
        for (var entity : state.level().getEntitiesForRendering()) {
            if (entity.getBoundingBox().maxY > state.minY() && entity.getBoundingBox().minY < state.maxY()) {
                entities.add(entity);
            }
        }
        var renderer = mc.getBlockRenderer();
        var previousLights = RenderSystem.getShaderLights();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
        pose.pushPose();
        try {
            pose.setIdentity();
            pose.translate((state.x1() - state.x0()) * guiScale / 2f,
                (state.y1() - state.y0()) * guiScale / 2f, 0);
            pose.scale(state.scale() * guiScale, -state.scale() * guiScale, state.scale() * guiScale);
            pose.mulPose(state.transform());
            // 相机位于视图矩阵中，实体光照保持在结构坐标系内。
            pose.last().normal().identity();
            // 半透明方块和流体在方块实体后绘制，避免提前写入深度而遮挡实体。
            for (int pass = 0; pass < 2; pass++) {
                boolean translucent = pass == 1;
                var positions = translucent ? blocks.stream().sorted(Comparator.comparingDouble(pos ->
                    state.transform().m02() * pos.getX() + state.transform().m12() * pos.getY()
                        + state.transform().m22() * pos.getZ())).toList() : blocks;
                for (var pos : positions) {
                    var block = view.getBlockState(pos);
                    pose.pushPose();
                    pose.translate(pos.getX(), pos.getY(), pos.getZ());
                    var random = net.minecraft.util.RandomSource.create(block.getSeed(pos));
                    var model = renderer.getBlockModel(block);
                    renderer.renderBatched(block, pos, view, pose,
                        layer -> (layer == ChunkSectionLayer.TRANSLUCENT) == translucent
                            ? this.bufferSource.getBuffer(layer == ChunkSectionLayer.TRANSLUCENT
                                ? Sheets.translucentItemSheet() : Sheets.cutoutBlockSheet())
                            : dev.anvilcraft.lib.v2.rendering.foundation.buffers.EmptyVertexConsumer.INSTANCE,
                        true, model.collectParts(view, pos, block, random));
                    var fluid = block.getFluidState();
                    if (!fluid.isEmpty() && (net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderLayer(fluid)
                        == ChunkSectionLayer.TRANSLUCENT) == translucent) {
                        var type = translucent ? Sheets.translucentItemSheet() : Sheets.cutoutBlockSheet();
                        renderer.renderLiquid(BlockPos.ZERO, view.shifted(pos),
                            new PosedConsumer(this.bufferSource.getBuffer(type), pose.last().copy()), block, fluid);
                    }
                    pose.popPose();
                }
                this.bufferSource.endBatch();
                if (!translucent) {
                    var camera = mc.gameRenderer.getLevelRenderState().cameraRenderState;
                    for (var entity : blockEntities) {
                        this.submitBlockEntity(entity, pose, camera);
                    }
                    for (var entity : entities) {
                        var entityState = mc.getEntityRenderDispatcher().extractEntity(entity, 0);
                        entityState.lightCoords = LightTexture.FULL_BRIGHT;
                        mc.getEntityRenderDispatcher().submit(entityState, camera, entity.getX(), entity.getY(), entity.getZ(), pose, this.nodes);
                    }
                    state.effects().submit(this.nodes, pose);
                    this.features.renderAllFeatures();
                    this.bufferSource.endBatch();
                }
            }
            this.features.endFrame();
            this.nodes.endFrame();
        } finally {
            this.nodes.clear();
            RenderSystem.setShaderLights(previousLights);
            pose.popPose();
        }
        state.effects().render(state);
    }

    private <T extends BlockEntity> void submitBlockEntity(T entity, PoseStack pose, CameraRenderState camera) {
        BlockEntityRenderer<T, BlockEntityRenderState> renderer = Minecraft.getInstance()
            .getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) {
            return;
        }
        BlockEntityRenderState state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, 0, Vec3.ZERO, null);
        state.lightCoords = LightTexture.FULL_BRIGHT;
        var pos = entity.getBlockPos();
        pose.pushPose();
        try {
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            renderer.submit(state, pose, this.nodes, camera);
        } finally {
            pose.popPose();
        }
    }

    /** 为单个预览提交附加模型，并在结构绘制完成后添加覆盖效果。 */
    public interface Effects {
        Effects NONE = new Effects() {};

        default void submit(net.minecraft.client.renderer.SubmitNodeCollector collector, PoseStack pose) {
        }

        default void render(State state) {
        }
    }

    public record State(SandboxRenderLevel level, int minY, int maxY, Matrix4f transform,
                        int x0, int y0, int x1, int y1, float scale,
                        @Nullable ScreenRectangle scissorArea, Effects effects) implements PictureInPictureRenderState {
        public State {
            java.util.Objects.requireNonNull(effects);
        }

        public State(SandboxRenderLevel level, int minY, int maxY, Matrix4f transform,
                     int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea) {
            this(level, minY, maxY, transform, x0, y0, x1, y1, scale, scissorArea, Effects.NONE);
        }

        @Override
        public Matrix3x2f pose() {
            return new Matrix3x2f();
        }
        @Override
        public @Nullable ScreenRectangle bounds() {
            var rectangle = new ScreenRectangle(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0);
            return this.scissorArea == null ? rectangle : rectangle.intersection(this.scissorArea);
        }
    }

    private record PosedConsumer(VertexConsumer delegate, PoseStack.Pose pose) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(this.pose, x, y, z);
            return this;
        }
        @Override
        public VertexConsumer setColor(int color) {
            this.delegate.setColor(color);
            return this;
        }
        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            this.delegate.setColor(r, g, b, a);
            return this;
        }
        @Override
        public VertexConsumer setUv(float u, float v) {
            this.delegate.setUv(u, v);
            return this;
        }
        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.delegate.setUv1(u, v);
            return this;
        }
        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.delegate.setUv2(u, v);
            return this;
        }
        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(this.pose, x, y, z);
            return this;
        }
        @Override
        public VertexConsumer setLineWidth(float width) {
            this.delegate.setLineWidth(width);
            return this;
        }
    }
}
