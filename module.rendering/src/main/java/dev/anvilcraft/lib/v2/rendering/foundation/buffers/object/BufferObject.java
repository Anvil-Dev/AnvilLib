package dev.anvilcraft.lib.v2.rendering.foundation.buffers.object;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import javax.annotation.Nonnull;

import java.nio.ByteBuffer;

public abstract class BufferObject<T extends BufferObject<T>> {

    protected final BufferLayout layout;
    protected final ShaderBufferObjectUsage usage;

    protected BufferObject(BufferLayout layout, ShaderBufferObjectUsage usage) {
        this.layout = layout;
        this.usage = usage;
    }

    protected abstract BufferObjectLayoutDefinition<T> getDefinition();

    /**
     * Writes this buffer object into the provided {@link ByteBuffer} using its layout.
     * <p>
     * Ported from 26.1: the original {@code upload(CommandEncoder, GpuBufferSlice)} (26.1 GPU pipeline)
     * and {@code DynamicUniformStorage} are not available on 1.21.1, so the object writes into a
     * user-provided buffer instead.
     */
    @SuppressWarnings("unchecked")
    public void write(@Nonnull ByteBuffer buffer) {
        getDefinition().write(buffer, (T) this, this.layout);
    }
}
