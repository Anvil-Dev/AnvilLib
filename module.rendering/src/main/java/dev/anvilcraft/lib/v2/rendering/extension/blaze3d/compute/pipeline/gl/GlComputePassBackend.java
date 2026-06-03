package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePassBackend;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;

public class GlComputePassBackend implements ALRComputePassBackend {
    @Override
    public void pushDebugGroup(String name) {

    }

    @Override
    public void popDebugGroup(String name) {

    }

    @Override
    public void dispatchWorkgroups(int groupCountX, int groupCountY, int groupCountZ) {

    }

    @Override
    public void dispatchWorkgroupsIndirect(GpuBuffer buffer, long offset) {

    }

    @Override
    public void memoryBarrier(MemoryBarrierFlag... flags) {

    }

    @Override
    public void bindTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource) {

    }

    @Override
    public void bindImage(int bindingPoint, GpuTexture resource, boolean read, boolean write) {

    }

    @Override
    public void bindUniformBlock(int bindingPoint, GpuBuffer resource) {

    }

    @Override
    public void bindShaderStorage(int bindingPoint, GpuBuffer resource) {

    }

    @Override
    public void bindAtomicCounter(int bindingPoint, GpuBuffer resource) {

    }

    @Override
    public void close() throws Exception {

    }
}
