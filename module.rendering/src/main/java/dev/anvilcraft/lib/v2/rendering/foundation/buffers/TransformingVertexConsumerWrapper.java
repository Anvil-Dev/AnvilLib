package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

public class TransformingVertexConsumerWrapper implements VertexConsumer {
    private final PoseStack.Pose pose;
    private final VertexConsumer delegate;

    public TransformingVertexConsumerWrapper(PoseStack.Pose pose, VertexConsumer delegate) {
        this.pose = pose;
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        return delegate.addVertex(pose.pose().transformPosition(x, y, z, new Vector3f()));
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return delegate.setColor(r, g, b, a);
    }

    @Override
    public VertexConsumer setColor(int color) {
        return delegate.setColor(color);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return delegate.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return delegate.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return delegate.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return delegate.setNormal(pose, x, y, z);
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return delegate.setLineWidth(width);
    }
}
