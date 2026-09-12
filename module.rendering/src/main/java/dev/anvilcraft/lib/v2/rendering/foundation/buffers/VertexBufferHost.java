package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.rendertype.RenderType;

public interface VertexBufferHost {
    GpuBuffer getVertexBuffer(RenderType renderType, long size);

    ByteBufferBuilder getSortingByteBufferBuilder(RenderType renderType);

    void acceptUploadAction(Runnable runnable);
}
