package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Owns one cached native projection and releases its GPU resources on replacement. */
public final class ProjectionRenderer implements AutoCloseable {
    private @Nullable ProjectionMesh mesh;
    private @Nullable ProjectionFeatures features;
    private final List<BlockEntity> blockEntities = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
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
        var mc = Minecraft.getInstance();
        var positions = view.positions();
        for (var pos : positions) {
            var entity = view.getBlockEntity(pos);
            if (entity != null) this.blockEntities.add(entity);
        }
        this.entities.addAll(view.entities());
        try (ByteBufferBuilder memory = new ByteBufferBuilder(2_097_152)) {
            var buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            VertexConsumer vertices = new GhostConsumer(buffer, BlockPos.ZERO, alpha);
            var pose = new PoseStack();
            var renderer = new ModelBlockRenderer(true, true, mc.getBlockColors());
            var fluids = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
            for (var pos : positions) {
                var state = view.realState(pos);
                renderer.tesselateBlock((x, y, z, quad, instance) -> {
                    pose.pushPose();
                    pose.translate(x, y, z);
                    vertices.putBakedQuad(pose.last(), quad, instance);
                    pose.popPose();
                }, pos.getX(), pos.getY(), pos.getZ(), view, pos, state,
                    mc.getModelManager().getBlockStateModelSet().get(state), state.getSeed(pos));
                if (!state.getFluidState().isEmpty()) {
                    fluids.tesselate(view.shifted(pos), BlockPos.ZERO, ignored -> new GhostConsumer(buffer, pos, alpha),
                        state, state.getFluidState());
                }
            }
            var data = buffer.build();
            if (data != null) this.mesh = new ProjectionMesh(data);
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
        if (this.mesh != null) this.mesh.draw(new Matrix4f(pose.last().pose()));
        if (this.features == null) this.features = new ProjectionFeatures();
        this.features.render(this.blockEntities, this.entities, pose, camera, this.opacity);
    }

    @Override
    public void close() {
        if (this.mesh != null) this.mesh.close();
        this.mesh = null;
        if (this.features != null) this.features.close();
        this.features = null;
        this.blockEntities.clear();
        this.entities.clear();
        this.built = false;
        this.bakedGeneration = -1;
    }

    record GhostConsumer(VertexConsumer delegate, BlockPos offset, int opacity) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x + this.offset.getX(), y + this.offset.getY(), z + this.offset.getZ());
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            this.delegate.setColor((color & 0xFFFFFF) | (this.opacity << 24));
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor(red, green, blue, this.opacity);
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
    }
}
