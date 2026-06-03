package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings;

import com.mojang.blaze3d.buffers.GpuBuffer;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;

public record UniformBlockBinding(
    String name
) implements ComputeBindingLayout<GpuBuffer> {
    @Override
    public ShaderResourceType type() {
        return ShaderResourceType.UNIFORM_BLOCK;
    }

    @Override
    public void apply(int bindingPoint, GpuBuffer resource, ALRComputePass computePass) {
        computePass.bindUniformBlock(bindingPoint, resource);
    }
}
