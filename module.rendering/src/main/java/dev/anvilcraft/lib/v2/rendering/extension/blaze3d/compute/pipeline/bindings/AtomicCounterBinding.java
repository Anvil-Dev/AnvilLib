package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.BufferSlice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;

public record AtomicCounterBinding(
    String name
) implements ComputeBindingLayout<BufferSlice> {
    @Override
    public ShaderResourceType type() {
        return ShaderResourceType.ATOMIC_COUNTER;
    }

    @Override
    public void apply(int bindingPoint, BufferSlice resource, ALRComputePass computePass) {
        computePass.bindAtomicCounter(bindingPoint, resource);
    }
}
