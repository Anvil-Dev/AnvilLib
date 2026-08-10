package dev.anvilcraft.lib.v2.rendering.postprocess;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Self-managed GL uniform buffer object (UBO) used to feed {@code layout(std140) uniform} blocks
 * of the post-processing shaders.
 * <p>
 * Ported from 26.1: the original relied on {@code GpuDevice.createBuffer} +
 * {@code CommandEncoder.writeToTexture}/{@code DynamicUniformStorage}, which do not exist on 1.21.1.
 * The data is still produced by the shared {@link BufferObject}/{@code Std140Writer} layout system
 * (see {@code foundation.buffers}), only the upload path now uses raw LWJGL buffer management.
 */
public final class GlUniformBuffer implements AutoCloseable {
    private final int bufferId;
    private final int size;

    public GlUniformBuffer(int size) {
        this.size = size;
        this.bufferId = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.bufferId);
        GlStateManager._glBufferData(GL31.GL_UNIFORM_BUFFER, size, GL31.GL_STREAM_DRAW);
    }

    /**
     * Writes the given buffer object into this UBO using its std140 layout and uploads it.
     */
    public void upload(BufferObject<?> object) {
        ByteBuffer buffer = ByteBuffer.allocate(this.size).order(ByteOrder.nativeOrder());
        object.write(buffer);
        GlStateManager._glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.bufferId);
        GlStateManager._glBufferData(GL31.GL_UNIFORM_BUFFER, buffer, GL31.GL_STREAM_DRAW);
    }

    /**
     * Binds this UBO to the uniform block {@code blockName} of the given program at {@code bindingPoint}.
     * The block index is looked up per program; if the program does not declare the block, nothing happens.
     */
    public void bindTo(int programId, String blockName, int bindingPoint) {
        int blockIndex = GL31.glGetUniformBlockIndex(programId, blockName);
        if (blockIndex == GL31.GL_INVALID_INDEX) {
            return;
        }
        GL31.glUniformBlockBinding(programId, blockIndex, bindingPoint);
        GL31.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, bindingPoint, this.bufferId, 0L, this.size);
    }

    @Override
    public void close() {
        GlStateManager._glDeleteBuffers(this.bufferId);
    }
}
