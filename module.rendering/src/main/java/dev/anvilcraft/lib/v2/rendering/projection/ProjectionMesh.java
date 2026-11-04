package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Objects;

final class ProjectionMesh implements AutoCloseable {
    private final ByteBuffer vertices;
    private final VertexFormat format;
    private final int[] vertexEnds;
    private final RenderType type;
    private @Nullable VertexBuffer buffer;
    private @Nullable BitSet visible;
    private boolean uploaded;
    private int vertexCount;

    ProjectionMesh(MeshData mesh, int[] vertexEnds) {
        this(mesh, vertexEnds, ProjectionRenderTypes.BLOCK_GHOST);
    }

    ProjectionMesh(MeshData mesh, int[] vertexEnds, RenderType type) {
        this.vertexEnds = vertexEnds;
        this.type = type;
        try (mesh) {
            this.format = mesh.drawState().format();
            this.vertices = MemoryUtil.memAlloc(mesh.vertexBuffer().remaining());
            this.vertices.put(mesh.vertexBuffer()).flip();
        }
    }

    void draw(Matrix4f pose, Matrix4f projection, @Nullable BitSet visible, boolean depthOnly) {
        if (visible != null && visible.isEmpty()) return;
        this.upload(visible);
        if (this.vertexCount == 0 || this.buffer == null) return;
        RenderType type = depthOnly ? ProjectionRenderTypes.ghost(this.type, true) : this.type;
        type.setupRenderState();
        try {
            this.buffer.bind();
            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose);
            this.buffer.drawWithShader(modelView, projection, RenderSystem.getShader());
        } finally {
            VertexBuffer.unbind();
            type.clearRenderState();
        }
    }

    private void upload(@Nullable BitSet visible) {
        if (this.uploaded && Objects.equals(this.visible, visible)) return;
        int stride = this.format.getVertexSize();
        this.vertexCount = 0;
        try (var memory = new ByteBufferBuilder(Math.max(256, this.vertices.remaining()))) {
            if (visible == null) {
                MemoryUtil.memCopy(MemoryUtil.memAddress(this.vertices),
                    memory.reserve(this.vertices.remaining()), this.vertices.remaining());
                this.vertexCount = this.vertices.remaining() / stride;
            } else {
                for (int index = visible.nextSetBit(0); index >= 0; index = visible.nextSetBit(index + 1)) {
                    int start = index == 0 ? 0 : this.vertexEnds[index - 1];
                    int count = this.vertexEnds[index] - start;
                    if (count == 0) continue;
                    MemoryUtil.memCopy(MemoryUtil.memAddress(this.vertices) + (long) start * stride,
                        memory.reserve(count * stride), (long) count * stride);
                    this.vertexCount += count;
                }
            }
            var result = memory.build();
            if (result != null) {
                if (this.buffer == null) this.buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                try {
                    this.buffer.bind();
                    this.buffer.upload(new MeshData(result, new MeshData.DrawState(this.format, this.vertexCount,
                        VertexFormat.Mode.QUADS.indexCount(this.vertexCount), VertexFormat.Mode.QUADS,
                        VertexFormat.IndexType.least(this.vertexCount))));
                } finally {
                    VertexBuffer.unbind();
                }
            }
        }
        this.visible = visible == null ? null : (BitSet) visible.clone();
        this.uploaded = true;
    }

    @Override
    public void close() {
        if (this.buffer != null) this.buffer.close();
        MemoryUtil.memFree(this.vertices);
    }
}
