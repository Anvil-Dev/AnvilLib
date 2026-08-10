package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.RenderType;

/**
 * Receives the compiled meshes of a {@link FullyBufferedBufferSource} (see {@link #acceptUploadAction}).
 * <p>
 * Ported from 26.1: the original returned {@code GpuBuffer}s; on 1.21.1 the host hands out
 * {@link VertexBuffer}s, which own both the vertex storage and the (possibly sorted) index storage.
 */
public interface VertexBufferHost {
    VertexBuffer getVertexBuffer(RenderType renderType, long size);

    ByteBufferBuilder getSortingByteBufferBuilder(RenderType renderType);

    void acceptUploadAction(Runnable runnable);
}
