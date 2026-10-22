package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.BitSet;
import java.util.OptionalDouble;
import java.util.OptionalInt;

final class ProjectionMesh implements AutoCloseable {
    private final GpuBuffer vertices;
    private final int indexCount;
    private final int[] vertexEnds;
    private final RenderType type;
    private @Nullable GpuBuffer filteredIndices;
    private @Nullable BitSet lastVisible;
    private boolean reversed;
    private int filteredIndexCount;

    ProjectionMesh(MeshData mesh, int[] vertexEnds) {
        this(mesh, vertexEnds, ProjectionRenderTypes.BLOCK_GHOST);
    }

    ProjectionMesh(MeshData mesh, int[] vertexEnds, RenderType type) {
        this.vertexEnds = vertexEnds;
        this.type = type;
        try (mesh) {
            this.vertices = RenderSystem.getDevice().createBuffer(() -> "AnvilLib structure projection",
                GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            this.indexCount = mesh.drawState().indexCount();
        }
    }

    void draw(Matrix4f pose) {
        this.draw(pose, null, false);
    }

    void draw(Matrix4f pose, @Nullable BitSet visible, boolean depthOnly) {
        boolean reversed = this.type.pipeline().isCull() && pose.determinant3x3() < 0;
        if (reversed && visible == null) {
            visible = new BitSet(this.vertexEnds.length);
            visible.set(0, this.vertexEnds.length);
        }
        if (visible != null) {
            if (visible.isEmpty()) return;
            this.updateIndices(visible, reversed);
            if (this.filteredIndexCount == 0) return;
        }
        var type = depthOnly ? ProjectionRenderTypes.BLOCK_DEPTH : this.type;
        var target = type.state.outputTarget.getRenderTarget();
        var color = RenderSystem.outputColorTextureOverride != null
            ? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
        var depth = RenderSystem.outputDepthTextureOverride != null
            ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView();
        var transforms = RenderSystem.getDynamicUniforms().writeTransform(
            new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose), new Vector4f(1), new Vector3f(), new Matrix4f());
        var sequential = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indices = sequential.getBuffer(this.indexCount);
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "AnvilLib structure projection", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
            pass.setPipeline(type.pipeline());
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            type.state.getTextures().forEach((name, texture) -> pass.bindTexture(name, texture.textureView(), texture.sampler()));
            pass.setVertexBuffer(0, this.vertices);
            if (visible == null) {
                pass.setIndexBuffer(indices, sequential.type());
                pass.drawIndexed(0, 0, this.indexCount, 1);
            } else {
                pass.setIndexBuffer(this.filteredIndices, VertexFormat.IndexType.INT);
                pass.drawIndexed(0, 0, this.filteredIndexCount, 1);
            }
        }
    }

    private void updateIndices(BitSet visible, boolean reversed) {
        if (visible.equals(this.lastVisible) && reversed == this.reversed) return;
        var data = MemoryUtil.memAlloc(this.indexCount * Integer.BYTES);
        try {
            for (int index = visible.nextSetBit(0); index >= 0; index = visible.nextSetBit(index + 1)) {
                int start = index == 0 ? 0 : this.vertexEnds[index - 1];
                for (int vertex = start; vertex < this.vertexEnds[index]; vertex += 4) {
                    int second = reversed ? vertex + 3 : vertex + 1;
                    int fourth = reversed ? vertex + 1 : vertex + 3;
                    data.putInt(vertex).putInt(second).putInt(vertex + 2);
                    data.putInt(vertex + 2).putInt(fourth).putInt(vertex);
                }
            }
            data.flip();
            this.filteredIndexCount = data.remaining() / Integer.BYTES;
            if (this.filteredIndexCount > 0) {
                if (this.filteredIndices == null) {
                    this.filteredIndices = RenderSystem.getDevice().createBuffer(() -> "AnvilLib projection visibility",
                        GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, (long) this.indexCount * Integer.BYTES);
                }
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.filteredIndices.slice(0, data.remaining()), data);
            }
            this.lastVisible = (BitSet) visible.clone();
            this.reversed = reversed;
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    static void reverseWinding(MeshData mesh) {
        if (mesh.drawState().mode() != VertexFormat.Mode.QUADS) return;
        var vertices = mesh.vertexBuffer();
        int stride = mesh.drawState().format().getVertexSize();
        for (int vertex = 0; vertex < mesh.drawState().vertexCount(); vertex += 4) {
            int second = (vertex + 1) * stride;
            int fourth = (vertex + 3) * stride;
            for (int offset = 0; offset < stride; offset++) {
                byte value = vertices.get(second + offset);
                vertices.put(second + offset, vertices.get(fourth + offset));
                vertices.put(fourth + offset, value);
            }
        }
    }

    @Override
    public void close() {
        this.vertices.close();
        if (this.filteredIndices != null) this.filteredIndices.close();
    }
}
