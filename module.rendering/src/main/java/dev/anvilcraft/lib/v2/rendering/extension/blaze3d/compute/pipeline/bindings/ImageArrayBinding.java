package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;

import java.util.List;

public record ImageArrayBinding (
    String name,
    boolean read,
    boolean write,
    int size
) implements ComputeBindingLayout<List<GpuTexture>> {

    public ImageArrayBinding {
        if (!read && !write) {
            throw new IllegalArgumentException("ImageResource does not allow both read and write are false");
        }
    }

    @Override
    public ShaderResourceType type() {
        return ShaderResourceType.TEXTURE_OR_IMAGE;
    }

    /// Just assume iterating over the list passed in is ordered.
    ///
    /// `resource.size <= this.size` is allowed, but `resource` must not have elements more than `this.size`
    @Override
    public int applyOrdered(int bindingPointStart, List<GpuTexture> resource, ALRComputePass computePass) {
        int increment = 0;
        Preconditions.checkArgument(this.size >= resource.size(), "resource must not have elements more than this.size");
        for (GpuTexture texture : resource) {
            computePass.bindImage(bindingPointStart + increment++, texture, read, write);
        }
        return increment;
    }
}
