package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ComputeBindingLayout;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.BindlessImageArrayBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;

import java.util.List;
import java.util.function.Supplier;

public class ALRComputePass implements AutoCloseable {
    private ALRComputePipeline pipeline;
    private final ALRComputePassBackend backend;

    public ALRComputePass(ALRComputePassBackend backend) {
        this.backend = backend;
    }

    public void pushDebugGroup(Supplier<String> name) {
        this.backend.pushDebugGroup(name);
    }

    public void popDebugGroup() {
        this.backend.popDebugGroup();
    }

    public void memoryBarrier(MemoryBarrierFlag... flags) {
        this.backend.memoryBarrier(flags);
    }

    public void dispatchWorkgroups(
        int groupCountX,
        int groupCountY,
        int groupCountZ
    ) {
        this.backend.pushDebugGroup(() -> "ALRComputePass " + pipeline.identifier());
        try {
            this.backend.dispatchWorkgroups(groupCountX, groupCountY, groupCountZ);
        } finally {
            this.backend.popDebugGroup();
        }
    }

    public void dispatchWorkgroupsIndirect(
        GpuBufferSlice buffer
    ) {
        this.backend.pushDebugGroup(() -> "ALRComputePass " + pipeline.identifier());
        try {
            this.backend.dispatchWorkgroupsIndirect(buffer);
        } finally {
            this.backend.popDebugGroup();
        }
    }

    @Override
    public void close() {
        this.backend.close();
    }

    public void setPipeline(ALRComputePipeline pipeline) {
        this.pipeline = pipeline;
        this.backend.setPipeline(pipeline);
    }

    public <T> int bind(int bindingPointStart, ComputeBindingLayout<T> layout, T resource) {
        return layout.applyOrdered(bindingPointStart, resource, this);
    }

    public void bindArrayOfTexture(int bindingPoint, List<GpuTexture> resource, boolean read, boolean write) {
        this.backend.bindArrayOfTexture(bindingPoint, resource, read, write);
    }

    public void bindTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource) {
        this.backend.bindTexture(bindingPoint, resource);
    }

    public void bindImage(int bindingPoint, GpuTexture resource, boolean read, boolean write) {
        this.backend.bindImage(bindingPoint, resource, read, write);
    }

    public void bindUniformBlock(int bindingPoint, GpuBufferSlice resource) {
        this.backend.bindUniformBlock(bindingPoint, resource);
    }

    public void bindShaderStorage(int bindingPoint, GpuBufferSlice resource) {
        this.backend.bindShaderStorage(bindingPoint, resource);
    }

    public void bindAtomicCounter(int bindingPoint, GpuBufferSlice resource) {
        this.backend.bindAtomicCounter(bindingPoint, resource);
    }

    public void bindBindlessImageArray(
        BindlessImageArrayBinding binding,
        List<GpuTexture> textures
    ) {
        this.backend.bindBindlessImageArray(binding, textures);
    }

    public void bindBindlessImageArray(String name, List<GpuTexture> textures) {
        this.backend.bindBindlessImageArray(name, textures);
    }
}
