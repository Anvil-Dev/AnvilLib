package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings;

import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;

public record ImageBinding(
    String name,
    boolean read,
    boolean write
) implements ComputeBindingLayout<GpuTexture> {

    public ImageBinding {
        if (!read && !write) {
            throw new IllegalArgumentException("ImageResource does not allow both read and write are false");
        }
    }

    @Override
    public ShaderResourceType type() {
        return ShaderResourceType.TEXTURE_OR_IMAGE;
    }

    @Override
    public int applyOrdered(int bindingPointStart, GpuTexture resource, ALRComputePass computePass) {
        computePass.bindImage(bindingPointStart, resource, read, write);
        return 1;
    }
}
