package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;

public record TextureBinding(
    String name
) implements ComputeBindingLayout<TextureBinding.SamplerAndTexture> {
    @Override
    public ShaderResourceType type() {
        return ShaderResourceType.TEXTURE_OR_IMAGE;
    }

    @Override
    public int applyOrdered(int bindingPointStart, SamplerAndTexture resource, ALRComputePass pass) {
        pass.bindTexture(bindingPointStart, resource);
        return 1;
    }

    public record SamplerAndTexture(
        GpuSampler sampler,
        GpuTexture texture
    ){}
}
