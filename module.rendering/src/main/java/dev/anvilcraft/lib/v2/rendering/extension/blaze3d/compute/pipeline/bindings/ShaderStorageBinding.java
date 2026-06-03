package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;

public record ShaderStorageBinding(
    String name
) implements ComputeBindingLayout<GpuBufferSlice> {
    @Override
    public ShaderResourceType type() {
        return ShaderResourceType.SHADER_STORAGE;
    }

    @Override
    public void apply(int bindingPoint, GpuBufferSlice resource, ALRComputePass computePass) {
        computePass.bindShaderStorage(bindingPoint, resource);
    }
}
