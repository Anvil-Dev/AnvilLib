package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ComputeBindingLayout;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import lombok.Setter;

import java.util.List;

public class ALRComputePass implements AutoCloseable {
    @Setter
    private ALRComputePipeline pipeline;
    private final ALRComputePassBackend backend;

    public ALRComputePass(ALRComputePassBackend backend) {
        this.backend = backend;
    }

    public void pushDebugGroup(String name) {
        this.backend.pushDebugGroup(name);
    }

    public void popDebugGroup(String name) {
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
        this.backend.dispatchWorkgroups(groupCountX, groupCountY, groupCountZ);
    }

    public void dispatchWorkgroupsIndirect(
        GpuBufferSlice buffer
    ) {
        this.backend.dispatchWorkgroupsIndirect(buffer);
    }

    @Override
    public void close() {
        this.backend.close();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void bindAll(List<?> elements) {
        int bindingPoint = 0;
        for (ComputeBindingLayout binding : pipeline.bindings()) {
            this.bind(bindingPoint++, binding, elements.get(bindingPoint));
        }
    }

    public <T> void bind(int bindingPoint, ComputeBindingLayout<T> layout, T resource) {
        layout.apply(bindingPoint, resource, this);
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
}
