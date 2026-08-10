package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;

/**
 * Binds a GL texture (by texture name) together with an optional GL sampler (0 = default sampler).
 * <p>
 * Ported from 26.1: {@code GpuSampler}/{@code GpuTexture} become plain GL ids; the sampler is
 * optional since 1.21.1's {@code GlStateManager} exposes no sampler management API.
 */
public record TextureBinding(
    String name
) implements ComputeBindingLayout<TextureBinding.SamplerAndTexture> {
    @Override
    public ShaderResourceType type() {
        return ShaderResourceType.TEXTURE;
    }

    @Override
    public void apply(int bindingPoint, SamplerAndTexture resource, ALRComputePass pass) {
        pass.bindTexture(bindingPoint, resource);
    }

    public record SamplerAndTexture(
        int samplerId,
        int textureId
    ) {
        public SamplerAndTexture(int textureId) {
            this(0, textureId);
        }
    }
}
