package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.BufferSlice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;

/**
 * Backend contract for {@link ALRComputePass}.
 * <p>
 * Ported from 26.1: {@code GpuBufferSlice}/{@code GpuTexture} resources become
 * {@link BufferSlice}/plain GL texture ids.
 */
public interface ALRComputePassBackend {
    void setPipeline(ALRComputePipeline pipeline);

    void pushDebugGroup(String name);

    void popDebugGroup();

    void dispatchWorkgroups(int groupCountX, int groupCountY, int groupCountZ);

    void dispatchWorkgroupsIndirect(BufferSlice buffer);

    void memoryBarrier(MemoryBarrierFlag... flags);

    void bindTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource);

    void bindImage(int bindingPoint, int textureId, boolean read, boolean write);

    void bindImage(int bindingPoint, int textureId, boolean read, boolean write, int internalFormat);

    void bindUniformBlock(int bindingPoint, BufferSlice resource);

    void bindShaderStorage(int bindingPoint, BufferSlice resource);

    void bindAtomicCounter(int bindingPoint, BufferSlice resource);

    void close();
}
