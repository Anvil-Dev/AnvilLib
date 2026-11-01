package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/** Owns one cached native projection and releases its GPU resources on replacement. */
public final class ProjectionRenderer implements AutoCloseable {
    private @Nullable ProjectionMesh mesh;
    private @Nullable ProjectionFeatures features;
    private final List<BlockEntity> blockEntities = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private @Nullable ProjectionScene scene;
    private List<BlockPos> positions = List.of();
    private final ProjectionOverlay overlay = new ProjectionOverlay();
    private boolean built;
    private int opacity;
    private int bakedGeneration = -1;
    private static int generation;

    static void invalidateResources() {
        generation++;
        ProjectionRenderTypes.clear();
    }

    public boolean isValid() {
        return this.built && this.bakedGeneration == generation;
    }

    /** Bakes local block/fluid geometry. The caller rebuilds after scene changes or resource invalidation. */
    public void rebuild(ProjectionScene view, int alpha) {
        if (alpha < 0 || alpha > 255) throw new IllegalArgumentException("Opacity must be between 0 and 255");
        this.close();
        this.scene = view;
        this.positions = view.positions();
        var positions = this.positions;
        int[] vertexEnds = new int[positions.size()];
        int[] vertexCount = {0};
        for (var pos : positions) {
            var entity = view.getBlockEntity(pos);
            if (entity != null) this.blockEntities.add(entity);
        }
        this.entities.addAll(view.entities());
        var mc = Minecraft.getInstance();
        try (ByteBufferBuilder memory = new ByteBufferBuilder(2_097_152)) {
            var buffer = new BufferBuilder(memory, PrimitiveTopology.QUADS, DefaultVertexFormat.BLOCK);
            VertexConsumer vertices = new GhostConsumer(buffer, BlockPos.ZERO, alpha, vertexCount);
            var pose = new PoseStack();
            var renderer = new ModelBlockRenderer(true, true, mc.getBlockColors());
            var fluids = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
            for (int index = 0; index < positions.size(); index++) {
                var pos = positions.get(index);
                var state = view.realState(pos);
                renderer.tesselateBlock((x, y, z, quad, instance) -> {
                    pose.pushPose();
                    pose.translate(x, y, z);
                    vertices.putBakedQuad(pose.last(), quad, instance);
                    pose.popPose();
                }, pos.getX(), pos.getY(), pos.getZ(), view, pos, state,
                    mc.getModelManager().getBlockStateModelSet().get(state), state.getSeed(pos));
                if (!state.getFluidState().isEmpty()) {
                    fluids.tesselate(view.shifted(pos), BlockPos.ZERO, ignored -> new GhostConsumer(buffer, pos, alpha, vertexCount),
                        state, state.getFluidState());
                }
                vertexEnds[index] = vertexCount[0];
            }
            var data = buffer.build();
            if (data != null) this.mesh = new ProjectionMesh(data, vertexEnds);
        } catch (RuntimeException | Error failure) {
            this.close();
            throw failure;
        }
        this.built = true;
        this.opacity = alpha;
        this.bakedGeneration = generation;
    }

    /** Draws in camera-relative space, preserving the current model-view transform. */
    public void render(PoseStack pose, CameraRenderState camera) {
        if (!this.isValid()) return;
        renderAll(List.of(new Prepared(pose, camera, this.blockEntities, null, new BitSet(), new BitSet())));
    }

    /**
     * Compares transformed local states with the world at {@code origin} on every draw.
     * Draw after world post-processing (for example, in {@code RenderLevelStageEvent.AfterLevel})
     * to keep the projection out of shader-pack lighting and color grading.
     */
    public void render(PoseStack pose, CameraRenderState camera, BlockGetter level, BlockPos origin) {
        var prepared = this.prepare(pose, camera, level, origin);
        if (prepared != null) renderAll(List.of(prepared));
    }

    /** Captures a placement for a shared depth prepass. Consume it before rebuilding this renderer. */
    public @Nullable Prepared prepare(PoseStack pose, CameraRenderState camera, BlockGetter level, BlockPos origin) {
        if (!this.isValid() || this.scene == null) return null;
        var missing = new BitSet(this.positions.size());
        var wrongStates = new BitSet(this.positions.size());
        var wrongBlocks = new BitSet(this.positions.size());
        var visibleEntities = new ArrayList<BlockEntity>();
        var worldPos = new BlockPos.MutableBlockPos();
        for (int index = 0; index < this.positions.size(); index++) {
            var pos = this.positions.get(index);
            var expected = this.scene.realState(pos);
            if (expected.isAir()) continue;
            worldPos.setWithOffset(origin, pos);
            var actual = level.getBlockState(worldPos);
            if (actual == expected) continue;
            if (actual.isAir()) {
                missing.set(index);
                var entity = this.scene.getBlockEntity(pos);
                if (entity != null) visibleEntities.add(entity);
            } else if (actual.is(expected.getBlock())) {
                wrongStates.set(index);
            } else {
                wrongBlocks.set(index);
            }
        }
        return new Prepared(pose, camera, visibleEntities, missing, wrongStates, wrongBlocks);
    }

    /** Resolves occlusion across blocks, block entities and all placements before blending any color. */
    public static void renderAll(List<Prepared> projections) {
        for (var projection : projections) projection.render(true);
        for (var projection : projections) projection.render(false);
        for (var projection : projections) projection.renderOverlay();
    }

    public final class Prepared {
        private final PoseStack pose = new PoseStack();
        private final CameraRenderState camera;
        private final List<BlockEntity> visibleEntities;
        private final @Nullable BitSet missing;
        private final BitSet wrongStates;
        private final BitSet wrongBlocks;

        private Prepared(PoseStack pose, CameraRenderState camera, List<BlockEntity> visibleEntities,
                         @Nullable BitSet missing, BitSet wrongStates, BitSet wrongBlocks) {
            this.pose.last().set(pose.last());
            this.camera = camera;
            this.visibleEntities = visibleEntities;
            this.missing = missing;
            this.wrongStates = wrongStates;
            this.wrongBlocks = wrongBlocks;
        }

        private void render(boolean depthOnly) {
            if (!ProjectionRenderer.this.isValid() || ProjectionRenderer.this.opacity == 0) return;
            var renderer = ProjectionRenderer.this;
            if (renderer.mesh != null) renderer.mesh.draw(this.pose.last().pose(), this.missing, depthOnly);
            if (renderer.features == null) renderer.features = new ProjectionFeatures();
            renderer.features.render(this.visibleEntities, renderer.entities, this.pose, this.camera, renderer.opacity, depthOnly);
        }

        private void renderOverlay() {
            if (!ProjectionRenderer.this.isValid()) return;
            ProjectionRenderer.this.overlay.render(this.pose.last().pose(), ProjectionRenderer.this.positions,
                this.wrongStates, this.wrongBlocks);
        }
    }

    @Override
    public void close() {
        if (this.mesh != null) this.mesh.close();
        this.mesh = null;
        if (this.features != null) this.features.close();
        this.features = null;
        this.overlay.close();
        this.scene = null;
        this.positions = List.of();
        this.blockEntities.clear();
        this.entities.clear();
        this.built = false;
        this.bakedGeneration = -1;
    }

    record GhostConsumer(VertexConsumer delegate, BlockPos offset, int opacity, int[] vertexCount) implements VertexConsumer {
        GhostConsumer(VertexConsumer delegate, BlockPos offset, int opacity) {
            this(delegate, offset, opacity, new int[1]);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x + this.offset.getX(), y + this.offset.getY(), z + this.offset.getZ());
            this.vertexCount[0]++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            this.delegate.setColor((color & 0xFFFFFF) | ((color >>> 24) * this.opacity / 255 << 24));
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor(red, green, blue, alpha * this.opacity / 255);
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
            this.delegate.setUv2(240, 240);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            this.delegate.setLineWidth(width);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }
        @Override
        public VertexConsumer setUv3(float u, float v) {
            this.delegate.setUv3(u, v);
            return this;
        }
    }
}
