package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reuses native entity render submission for both opaque previews and translucent projections. */
public final class ProjectionFeatures implements AutoCloseable {
    private boolean closed;
    private final SubmitNodeStorage nodes = new SubmitNodeStorage();
    private final GhostBuffers buffers = new GhostBuffers();
    private final FeatureRenderDispatcher features;
    private final Map<BlockEntity, BlockEntityRenderState> states = new IdentityHashMap<>();

    public ProjectionFeatures() {
        var client = Minecraft.getInstance();
        var renderBuffers = client.renderBuffers();
        this.features = new FeatureRenderDispatcher(this.nodes, client.getBlockRenderer(), this.buffers, client.getAtlasManager(),
            renderBuffers.outlineBufferSource(), renderBuffers.crumblingBufferSource(),
            client.font);
    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;
        this.buffers.memory.close();
        this.features.close();
        this.clear();
    }

    public void clear() {
        this.states.clear();
        this.nodes.clear();
    }

    public void render(List<BlockEntity> blockEntities, List<Entity> entities, PoseStack pose, CameraRenderState camera, int alpha) {
        this.render(blockEntities, entities, pose, camera, alpha, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void render(List<BlockEntity> blockEntities, List<Entity> entities, PoseStack pose,
                CameraRenderState camera, int alpha, boolean depthOnly) {
        this.buffers.alpha = alpha;
        this.buffers.depthOnly = depthOnly;
        this.buffers.reversed = pose.last().pose().determinant3x3() < 0;
        this.states.keySet().retainAll(blockEntities);
        var client = Minecraft.getInstance();
        for (BlockEntity entity : blockEntities) {
            BlockEntityRenderer renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
            if (renderer == null) continue;
            BlockEntityRenderState state = this.states.computeIfAbsent(entity, ignored -> renderer.createRenderState());
            renderer.extractRenderState(entity, state, 0, Vec3.ZERO, null);
            state.lightCoords = LightTexture.FULL_BRIGHT;
            pose.pushPose();
            try {
                var pos = entity.getBlockPos();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                renderer.submit(state, pose, this.nodes, camera);
            } finally {
                pose.popPose();
            }
        }
        for (Entity entity : entities) {
            var state = client.getEntityRenderDispatcher().extractEntity(entity, 0);
            state.lightCoords = LightTexture.FULL_BRIGHT;
            client.getEntityRenderDispatcher().submit(state, camera, entity.getX(), entity.getY(), entity.getZ(), pose, this.nodes);
        }
        this.features.renderAllFeatures();
        this.buffers.endBatch();
        this.features.endFrame();
        this.nodes.endFrame();
    }

    private static final class GhostBuffers extends MultiBufferSource.BufferSource {
        private int alpha;
        private boolean depthOnly;
        private boolean reversed;
        private final ByteBufferBuilder memory;

        private GhostBuffers() {
            this(new ByteBufferBuilder(256));
        }

        private GhostBuffers(ByteBufferBuilder memory) {
            super(memory, new LinkedHashMap<>());
            this.memory = memory;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            if (this.alpha == 255 && !this.depthOnly) return super.getBuffer(type);
            return new ProjectionRenderer.GhostConsumer(
                super.getBuffer(ProjectionRenderTypes.ghost(type, this.depthOnly)), BlockPos.ZERO, this.alpha);
        }

        @Override
        public void endBatch(RenderType type) {
            var builder = this.startedBuilders.remove(type);
            if (builder != null) {
                var mesh = builder.build();
                if (mesh != null) {
                    if (this.reversed && type.pipeline().isCull()) ProjectionMesh.reverseWinding(mesh);
                    if (type.sortOnUpload()) mesh.sortQuads(this.memory, RenderSystem.getProjectionType().vertexSorting());
                    type.draw(mesh);
                }
            }
            if (type.equals(this.lastSharedType)) this.lastSharedType = null;
        }
    }
}
