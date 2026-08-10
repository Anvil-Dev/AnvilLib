package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.BufferSlice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ComputeBindingLayout;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;

import java.util.List;

/**
 * A single compute dispatch session backed by an {@link ALRComputePassBackend}.
 * <p>
 * Ported from 26.1: {@code GpuBufferSlice}/{@code GpuTexture} resources become
 * {@link BufferSlice}/plain GL texture ids.
 */
public class ALRComputePass implements AutoCloseable {
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
        this.backend.pushDebugGroup("Compute " + pipeline.identifier());
        this.backend.dispatchWorkgroups(groupCountX, groupCountY, groupCountZ);
        this.backend.popDebugGroup();
    }

    public void dispatchWorkgroupsIndirect(
        BufferSlice buffer
    ) {
        this.backend.dispatchWorkgroupsIndirect(buffer);
    }

    @Override
    public void close() {
        this.backend.close();
    }

    public void setPipeline(ALRComputePipeline pipeline) {
        this.pipeline = pipeline;
        this.backend.setPipeline(pipeline);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void bindAll(List<?> elements) {
        int bindingPoint = 0;
        for (ComputeBindingLayout binding : pipeline.bindings()) {
            this.bind(bindingPoint++, binding, elements.get(bindingPoint - 1));
        }
    }

    public <T> void bind(int bindingPoint, ComputeBindingLayout<T> layout, T resource) {
        layout.apply(bindingPoint, resource, this);
    }

    public void bindTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource) {
        this.backend.bindTexture(bindingPoint, resource);
    }

    public void bindImage(int bindingPoint, int textureId, boolean read, boolean write) {
        this.backend.bindImage(bindingPoint, textureId, read, write);
    }

    /**
     * Binds a texture as an image with an explicit internal format for {@code glBindImageTexture}.
     */
    public void bindImage(int bindingPoint, int textureId, boolean read, boolean write, int internalFormat) {
        this.backend.bindImage(bindingPoint, textureId, read, write, internalFormat);
    }

    public void bindUniformBlock(int bindingPoint, BufferSlice resource) {
        this.backend.bindUniformBlock(bindingPoint, resource);
    }

    public void bindShaderStorage(int bindingPoint, BufferSlice resource) {
        this.backend.bindShaderStorage(bindingPoint, resource);
    }

    public void bindAtomicCounter(int bindingPoint, BufferSlice resource) {
        this.backend.bindAtomicCounter(bindingPoint, resource);
    }
}
