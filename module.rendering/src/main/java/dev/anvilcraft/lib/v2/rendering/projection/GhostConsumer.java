package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;

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
    public VertexConsumer setNormal(float x, float y, float z) {
        this.delegate.setNormal(x, y, z);
        return this;
    }
}
