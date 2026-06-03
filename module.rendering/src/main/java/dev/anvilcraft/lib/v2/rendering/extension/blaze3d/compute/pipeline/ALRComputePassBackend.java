package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;

public interface ALRComputePassBackend {
    void setPipeline(ALRComputePipeline pipeline);

    void pushDebugGroup(String name);

    void popDebugGroup();

    void dispatchWorkgroups(int groupCountX, int groupCountY, int groupCountZ);

    void dispatchWorkgroupsIndirect(GpuBufferSlice buffer);

    void memoryBarrier(MemoryBarrierFlag... flags);

    void bindTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource);

    void bindImage(int bindingPoint, GpuTexture resource, boolean read, boolean write);

    void bindUniformBlock(int bindingPoint, GpuBufferSlice resource);

    void bindShaderStorage(int bindingPoint, GpuBufferSlice resource);

    void bindAtomicCounter(int bindingPoint, GpuBufferSlice resource);

    void close();
}
