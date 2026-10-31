package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;

/** 复用旧版实体绘制入口，并用独立缓冲共享深度预绘制。 */
public final class ProjectionFeatures implements AutoCloseable {
    private final GhostBuffers buffers = new GhostBuffers();

    public void render(List<BlockEntity> blockEntities, List<Entity> entities, PoseStack pose, int alpha) {
        this.render(blockEntities, entities, pose, alpha, false);
    }

    void render(List<BlockEntity> blockEntities, List<Entity> entities, PoseStack pose, int alpha, boolean depthOnly) {
        this.buffers.alpha = alpha;
        this.buffers.depthOnly = depthOnly;
        this.buffers.reversed = pose.last().pose().determinant3x3() < 0;
        var client = Minecraft.getInstance();
        for (BlockEntity entity : blockEntities) {
            var renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
            if (renderer == null) continue;
            pose.pushPose();
            try {
                var pos = entity.getBlockPos();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                renderer.render(entity, 0, pose, this.buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Vec3.ZERO);
            } finally {
                pose.popPose();
            }
        }
        for (Entity entity : entities) {
            client.getEntityRenderDispatcher().render(entity, entity.getX(), entity.getY(), entity.getZ(),
                0, pose, this.buffers, LightTexture.FULL_BRIGHT);
        }
        this.buffers.endBatch();
    }

    @Override
    public void close() { this.buffers.memory.close(); }

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
                    if (this.reversed && ProjectionRenderTypes.culls(type)) ProjectionMesh.reverseWinding(mesh);
                    if (type.sortOnUpload()) mesh.sortQuads(this.memory, RenderSystem.getProjectionType().vertexSorting());
                    type.draw(mesh);
                }
            }
            if (type.equals(this.lastSharedType)) this.lastSharedType = null;
        }
    }
}
