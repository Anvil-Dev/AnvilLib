package dev.anvilcraft.lib.v2.renderer.projection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * An independently owned, reusable projection mesh. Call on the render thread only.
 * Rebuild after scene changes or when {@link #isValid()} becomes false after resource reload.
 * The caller positions the pose in camera-relative space and closes the renderer when finished.
 */
public final class ProjectionRenderer implements AutoCloseable {
    private @Nullable VertexBuffer mesh;
    private List<BlockEntity> blockEntities = List.of();
    private List<Entity> entities = List.of();
    private int opacity;
    private int generation = -1;

    public boolean isValid() {
        return this.generation == ProjectionShaders.generation();
    }

    /** Bakes a complete scene; opacity is in the inclusive range 0 to 255. */
    public void rebuild(ProjectionScene view, int alpha) {
        RenderSystem.assertOnRenderThread();
        if (alpha < 0 || alpha > 255) throw new IllegalArgumentException("Opacity must be between 0 and 255");
        this.close();
        Minecraft mc = Minecraft.getInstance();
        try (ByteBufferBuilder memory = new ByteBufferBuilder(2_097_152)) {
            BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            VertexConsumer vertices = new GhostConsumer(buffer, BlockPos.ZERO, alpha);
            PoseStack pose = new PoseStack();
            RandomSource random = RandomSource.create();
            for (BlockPos pos : view.positions()) {
                var state = view.realState(pos);
                if (state.isAir()) continue;
                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                ModelData data = view.getModelData(pos);
                var model = mc.getBlockRenderer().getBlockModel(state);
                for (RenderType type : model.getRenderTypes(state, random, data)) {
                    mc.getBlockRenderer().renderBatched(state, pos, view, pose, vertices, true, random, data, type);
                }
                pose.popPose();
                if (!state.getFluidState().isEmpty()) {
                    mc.getBlockRenderer().renderLiquid(BlockPos.ZERO, view.shifted(pos), new GhostConsumer(buffer, pos, alpha),
                        state, state.getFluidState());
                }
            }
            MeshData data = buffer.build();
            if (data != null) {
                this.mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
                try {
                    this.mesh.bind();
                    this.mesh.upload(data);
                } finally {
                    VertexBuffer.unbind();
                }
            }
        } catch (RuntimeException | Error exception) {
            this.close();
            throw exception;
        }
        this.blockEntities = view.positions().stream().map(view::getBlockEntity).filter(Objects::nonNull).toList();
        this.entities = view.entities();
        this.opacity = alpha;
        this.generation = ProjectionShaders.generation();
    }

    /** Draws the cached mesh and live entity renderers, then flushes the supplied buffers. */
    public void render(PoseStack pose, Matrix4f projectionMatrix, MultiBufferSource.BufferSource buffers) {
        RenderSystem.assertOnRenderThread();
        if (!this.isValid()) return;
        Minecraft mc = Minecraft.getInstance();
        if (this.mesh != null) {
            RenderType type = ProjectionRenderTypes.ghost(RenderType.translucent());
            type.setupRenderState();
            try {
                this.mesh.bind();
                // Cached meshes need the camera view matrix used by the entity buffer path.
                Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
                this.mesh.drawWithShader(modelView, projectionMatrix, RenderSystem.getShader());
            } finally {
                VertexBuffer.unbind();
                type.clearRenderState();
            }
        }
        MultiBufferSource ghostBuffers = type -> new GhostConsumer(
            buffers.getBuffer(ProjectionRenderTypes.ghost(type)), BlockPos.ZERO, this.opacity);
        for (BlockEntity entity : this.blockEntities) {
            pose.pushPose();
            try {
                BlockPos pos = entity.getBlockPos();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                mc.getBlockEntityRenderDispatcher().renderItem(entity, pose, ghostBuffers,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } finally {
                pose.popPose();
            }
        }
        for (Entity entity : this.entities) {
            mc.getEntityRenderDispatcher().render(entity, entity.getX(), entity.getY(), entity.getZ(),
                entity.getYRot(), 0, pose, ghostBuffers, LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
    }

    @Override
    public void close() {
        if (this.mesh != null) {
            this.mesh.close();
            this.mesh = null;
        }
        this.blockEntities = List.of();
        this.entities = List.of();
        this.generation = -1;
    }
}
