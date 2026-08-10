package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;

/**
 * Binds a GL texture (given by its texture name) as an image for read/write access.
 * <p>
 * Ported from 26.1: {@code GpuTexture} becomes a plain GL texture id. The default internal format
 * used for {@code glBindImageTexture} is {@code GL_RGBA8}; {@link ALRComputePass#bindImage} offers
 * an overload accepting an explicit internal format.
 */
public record ImageBinding(
    String name,
    boolean read,
    boolean write
) implements ComputeBindingLayout<Integer> {

    public ImageBinding {
        if (!read && !write) {
            throw new IllegalArgumentException("ImageResource does not allow both read and write are false");
        }
    }

    @Override
    public ShaderResourceType type() {
        return ShaderResourceType.IMAGE;
    }

    @Override
    public void apply(int bindingPoint, Integer resource, ALRComputePass computePass) {
        computePass.bindImage(bindingPoint, resource, read, write);
    }
}
