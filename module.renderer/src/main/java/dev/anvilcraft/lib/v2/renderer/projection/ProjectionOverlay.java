package dev.anvilcraft.lib.v2.renderer.projection;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.List;

final class ProjectionOverlay implements AutoCloseable {
    private static final float EXPANSION = 0.05F;
    private static final int WRONG_STATE_COLOR = 0x70FFCC00;
    private static final int WRONG_BLOCK_COLOR = 0x70FF3333;
    private BitSet wrongStates = new BitSet();
    private BitSet wrongBlocks = new BitSet();
    private @Nullable ProjectionMesh mesh;

    void render(Matrix4f pose, Matrix4f projection, List<BlockPos> positions, BitSet states, BitSet blocks) {
        if (!this.wrongStates.equals(states) || !this.wrongBlocks.equals(blocks)) {
            this.close();
            try (var memory = new ByteBufferBuilder(786432)) {
                var buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                for (int index = states.nextSetBit(0); index >= 0; index = states.nextSetBit(index + 1)) {
                    box(buffer, positions.get(index), WRONG_STATE_COLOR);
                }
                for (int index = blocks.nextSetBit(0); index >= 0; index = blocks.nextSetBit(index + 1)) {
                    box(buffer, positions.get(index), WRONG_BLOCK_COLOR);
                }
                var data = buffer.build();
                if (data != null) this.mesh = new ProjectionMesh(data, new int[0], ProjectionRenderTypes.OVERLAY);
            }
            this.wrongStates = (BitSet) states.clone();
            this.wrongBlocks = (BitSet) blocks.clone();
        }
        if (this.mesh != null) this.mesh.draw(pose, projection, null, false);
    }

    private static void box(VertexConsumer vertices, BlockPos pos, int color) {
        float x0 = pos.getX() - EXPANSION;
        float y0 = pos.getY() - EXPANSION;
        float z0 = pos.getZ() - EXPANSION;
        float x1 = pos.getX() + 1 + EXPANSION;
        float y1 = pos.getY() + 1 + EXPANSION;
        float z1 = pos.getZ() + 1 + EXPANSION;
        vertices.addVertex(x0, y0, z0).setColor(color);
        vertices.addVertex(x1, y0, z0).setColor(color);
        vertices.addVertex(x1, y0, z1).setColor(color);
        vertices.addVertex(x0, y0, z1).setColor(color);
        vertices.addVertex(x0, y1, z0).setColor(color);
        vertices.addVertex(x0, y1, z1).setColor(color);
        vertices.addVertex(x1, y1, z1).setColor(color);
        vertices.addVertex(x1, y1, z0).setColor(color);
        vertices.addVertex(x0, y0, z0).setColor(color);
        vertices.addVertex(x0, y1, z0).setColor(color);
        vertices.addVertex(x1, y1, z0).setColor(color);
        vertices.addVertex(x1, y0, z0).setColor(color);
        vertices.addVertex(x0, y0, z1).setColor(color);
        vertices.addVertex(x1, y0, z1).setColor(color);
        vertices.addVertex(x1, y1, z1).setColor(color);
        vertices.addVertex(x0, y1, z1).setColor(color);
        vertices.addVertex(x0, y0, z0).setColor(color);
        vertices.addVertex(x0, y0, z1).setColor(color);
        vertices.addVertex(x0, y1, z1).setColor(color);
        vertices.addVertex(x0, y1, z0).setColor(color);
        vertices.addVertex(x1, y0, z0).setColor(color);
        vertices.addVertex(x1, y1, z0).setColor(color);
        vertices.addVertex(x1, y1, z1).setColor(color);
        vertices.addVertex(x1, y0, z1).setColor(color);
    }

    @Override
    public void close() {
        if (this.mesh != null) this.mesh.close();
        this.mesh = null;
        this.wrongStates.clear();
        this.wrongBlocks.clear();
    }
}
