package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;

final class ProjectionBuffers extends MultiBufferSource.BufferSource implements AutoCloseable {
    private int opacity;
    private boolean depthOnly;

    ProjectionBuffers() {
        super(new ByteBufferBuilder(786432), new LinkedHashMap<>());
    }

    void prepare(int opacity, boolean depthOnly) {
        this.opacity = opacity;
        this.depthOnly = depthOnly;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return new GhostConsumer(super.getBuffer(ProjectionRenderTypes.ghost(type, this.depthOnly)), BlockPos.ZERO, this.opacity);
    }

    @Override
    public void close() {
        this.sharedBuffer.close();
    }
}
