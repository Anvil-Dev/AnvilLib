package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;

public record AtomicCounterBinding(
    String name
) implements ComputeBindingLayout<GpuBufferSlice> {
    @Override
    public ShaderResourceType type() {
        return ShaderResourceType.ATOMIC_COUNTER;
    }

    @Override
    public int applyOrdered(int bindingPointStart, GpuBufferSlice resource, ALRComputePass computePass) {
        computePass.bindAtomicCounter(bindingPointStart, resource);
        return 1;
    }
}
