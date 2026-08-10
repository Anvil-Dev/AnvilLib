package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute;

import org.jetbrains.annotations.ApiStatus;

/**
 * A slice of a GL buffer, described by the raw buffer handle, byte offset and byte length.
 * <p>
 * Ported from 26.1: the original used {@code com.mojang.blaze3d.buffers.GpuBufferSlice}, which does
 * not exist on 1.21.1. The buffer handle is the plain LWJGL buffer name (see
 * {@code GlStateManager._glGenBuffers}).
 */
@ApiStatus.Internal
public record BufferSlice(int bufferId, long offset, long length) {
    public static BufferSlice of(int bufferId, long length) {
        return new BufferSlice(bufferId, 0L, length);
    }

    public static BufferSlice of(int bufferId, long offset, long length) {
        return new BufferSlice(bufferId, offset, length);
    }
}
