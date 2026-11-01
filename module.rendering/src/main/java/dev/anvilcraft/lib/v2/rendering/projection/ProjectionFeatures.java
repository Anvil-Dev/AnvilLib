package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.StagedVertexBuffer;
import dev.anvilcraft.lib.v2.rendering.foundation.FeatureGeometryCapture;
import dev.anvilcraft.lib.v2.rendering.foundation.FeatureRendering;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
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
    private final RenderBuffers renderBuffers = new RenderBuffers(1);
    private final FeatureRenderDispatcher features;
    private final Map<BlockEntity, BlockEntityRenderState> states = new IdentityHashMap<>();

    public ProjectionFeatures() {
        this.features = FeatureRendering.dispatcher(this.renderBuffers);
    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;
        this.buffers.close();
        this.renderBuffers.close();
        this.features.close();
        this.clear();
    }

    public void clear() {
        this.states.clear();
        FeatureRendering.clear(this.nodes);
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
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
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
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            client.getEntityRenderDispatcher().submit(state, camera, entity.getX(), entity.getY(), entity.getZ(), pose, this.nodes);
        }
        try (FeatureGeometryCapture capture = new FeatureGeometryCapture(this.buffers::getBuffer);
             var frame = this.features.prepareFrame(this.nodes)) {
            // Feature preparation writes the same native geometry into opacity-aware buffers.
        }
        this.buffers.endBatch();
        this.renderBuffers.endFrame();
    }

    private static final class GhostBuffers implements AutoCloseable {
        private int alpha;
        private boolean depthOnly;
        private boolean reversed;
        private final Map<RenderType, ByteBufferBuilder> memory = new LinkedHashMap<>();
        private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();

        public VertexConsumer getBuffer(RenderType original) {
            RenderType type = this.alpha == 255 && !this.depthOnly ? original : ProjectionRenderTypes.ghost(original, this.depthOnly);
            var builder = this.builders.computeIfAbsent(type, key -> new BufferBuilder(
                this.memory.computeIfAbsent(key, ignored -> new ByteBufferBuilder(1024)), key.primitiveTopology(), key.format()));
            return this.alpha == 255 && !this.depthOnly ? builder : new ProjectionRenderer.GhostConsumer(builder, BlockPos.ZERO, this.alpha);
        }

        public void endBatch() {
            var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            for (var entry : this.builders.entrySet()) {
                RenderType type = entry.getKey();
                var mesh = entry.getValue().build();
                if (mesh == null) continue;
                try (mesh) {
                    if (this.reversed && type.pipeline().isCull()) ProjectionMesh.reverseWinding(mesh);
                    if (type.sortOnUpload()) mesh.sortQuads(this.memory.get(type), RenderSystem.getProjectionType().vertexSorting());
                    var device = RenderSystem.getDevice();
                    var state = mesh.drawState();
                    var sequential = RenderSystem.getSequentialBuffer(type.primitiveTopology());
                    var sequentialIndices = sequential.getBuffer(state.indexCount());
                    try (var vertices = device.createBuffer(() -> "Projection entity vertices", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                         var indices = mesh.indexBuffer() == null ? null : device.createBuffer(
                             () -> "Projection entity indices", GpuBuffer.USAGE_INDEX, mesh.indexBuffer())) {
                        var prepared = type.prepare();
                        try (var pass = device.createCommandEncoder().createRenderPass(() -> "Projection entities",
                            target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
                            prepared.drawFromBuffer(new StagedVertexBuffer.ExecuteInfo(vertices, indices == null ? sequentialIndices : indices,
                                indices == null ? sequential.type() : state.indexType(), 0, 0, state.indexCount(), type.primitiveTopology()), pass);
                        }
                    }
                }
                this.memory.get(type).clear();
            }
            this.builders.clear();
        }

        @Override
        public void close() {
            this.memory.values().forEach(ByteBufferBuilder::close);
            this.memory.clear();
            this.builders.clear();
        }
    }
}
