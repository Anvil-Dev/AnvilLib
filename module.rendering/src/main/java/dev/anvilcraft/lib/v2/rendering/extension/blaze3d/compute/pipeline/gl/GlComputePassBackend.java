package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.gl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePassBackend;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstance;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.ARBShaderAtomicCounters;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL46;

@ApiStatus.Internal
public class GlComputePassBackend implements ALRComputePassBackend {
    private final ALRGpuDeviceBackendExtension backendExtension;
    private final ALRCommandEncoderBackendExtension commandEncoderExtension;
    private final Int2ObjectMap<TextureBinding.SamplerAndTexture> textureBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ImageState> imageBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<GpuBufferSlice> uniformBlockBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<GpuBufferSlice> shaderStorageBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<GpuBufferSlice> atomicCounterBindings = new Int2ObjectOpenHashMap<>();
    private ALRComputePipeline pipeline = null;

    public GlComputePassBackend(
        ALRCommandEncoderBackendExtension commandEncoderExtension,
        ALRGpuDeviceBackendExtension backendExtension
    ) {
        this.commandEncoderExtension = commandEncoderExtension;
        this.backendExtension = backendExtension;
    }

    @Override
    public void setPipeline(ALRComputePipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void pushDebugGroup(String name) {
        this.backendExtension.alrPushDebugGroup(name);
    }

    @Override
    public void popDebugGroup() {
        this.backendExtension.alrPopDebugGroup();
    }

    @Override
    public void dispatchWorkgroups(int groupCountX, int groupCountY, int groupCountZ) {
        this.setupState();
        this.commandEncoderExtension.alrDispatchWorkgroups(groupCountX, groupCountY, groupCountZ);
    }

    @Override
    public void dispatchWorkgroupsIndirect(GpuBufferSlice buffer) {
        this.setupState();
        this.commandEncoderExtension.alrDispatchWorkgroupsIndirect(buffer);
    }

    @Override
    public void memoryBarrier(MemoryBarrierFlag... flags) {
        this.commandEncoderExtension.alrMemoryBarrier(flags);
    }

    @Override
    public void bindTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource) {
        this.textureBindings.put(bindingPoint, resource);
    }

    @Override
    public void bindImage(int bindingPoint, GpuTexture resource, boolean read, boolean write) {
        if (!read && !write) {
            throw new IllegalArgumentException("ImageResource does not allow both read and write are false");
        }
        this.imageBindings.put(bindingPoint, new ImageState(resource, read, write));
    }

    @Override
    public void bindUniformBlock(int bindingPoint, GpuBufferSlice resource) {
        this.uniformBlockBindings.put(bindingPoint, resource);
    }

    @Override
    public void bindShaderStorage(int bindingPoint, GpuBufferSlice resource) {
        this.shaderStorageBindings.put(bindingPoint, resource);
    }

    @Override
    public void bindAtomicCounter(int bindingPoint, GpuBufferSlice resource) {
        this.atomicCounterBindings.put(bindingPoint, resource);
    }

    @Override
    public void close() {

    }

    private void setupState() {
        ALRComputeProgramInstance program = ALRComputeShaderManager.INSTANCE.getShader(pipeline);
        if (program == null || program == ALRComputeProgramInstance.INVALID) return;
        GL46.glUseProgram(program.id());

        for (Int2ObjectMap.Entry<TextureBinding.SamplerAndTexture> entry : this.textureBindings.int2ObjectEntrySet()) {
            this.setupTexture(entry.getIntKey(), entry.getValue());
        }
        for (Int2ObjectMap.Entry<ImageState> entry : this.imageBindings.int2ObjectEntrySet()) {
            this.setupImage(entry.getIntKey(), entry.getValue());
        }
        for (Int2ObjectMap.Entry<GpuBufferSlice> entry : this.uniformBlockBindings.int2ObjectEntrySet()) {
            this.setupUniformBlock(entry.getIntKey(), entry.getValue());
        }
        for (Int2ObjectMap.Entry<GpuBufferSlice> entry : this.shaderStorageBindings.int2ObjectEntrySet()) {
            this.setupShaderStorage(entry.getIntKey(), entry.getValue());
        }
        for (Int2ObjectMap.Entry<GpuBufferSlice> entry : this.atomicCounterBindings.int2ObjectEntrySet()) {
            this.setupAtomicCounter(entry.getIntKey(), entry.getValue());
        }
    }

    private void setupTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource) {
        int sampler = ((GlSampler) resource.sampler()).getId();
        int texture = ((GlTexture) resource.texture()).glId();

        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + bindingPoint);
        GlStateManager._bindTexture(texture);
        GL33.glBindSampler(bindingPoint, sampler);
    }

    private void setupImage(int bindingPoint, ImageState state) {
        int access = 0;
        if (state.read() && state.write()) {
            access = GL46.GL_READ_WRITE;
        } else {
            if (state.read()) {
                access |= GL46.GL_READ_ONLY;
            }
            if (state.write()) {
                access |= GL46.GL_WRITE_ONLY;
            }
        }

        ARBShaderImageLoadStore.glBindImageTexture(
            bindingPoint,
            ((GlTexture) state.resource()).glId(),
            0,
            false,
            0,
            access,
            GlConst.toGlInternalId(state.resource().getFormat())
        );
    }

    private void setupUniformBlock(int bindingPoint, GpuBufferSlice resource) {
        GL46.glBindBufferRange(
            GL46.GL_UNIFORM_BUFFER,
            bindingPoint,
            ((GlBuffer) resource.buffer()).handle,
            resource.offset(),
            resource.length()
        );
    }

    private void setupShaderStorage(int bindingPoint, GpuBufferSlice resource) {
        GL46.glBindBufferRange(
            ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER,
            bindingPoint,
            ((GlBuffer) resource.buffer()).handle,
            resource.offset(),
            resource.length()
        );
    }

    private void setupAtomicCounter(int bindingPoint, GpuBufferSlice resource) {
        GL46.glBindBufferRange(
            ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER,
            bindingPoint,
            ((GlBuffer) resource.buffer()).handle,
            resource.offset(),
            resource.length()
        );
    }

    private record ImageState(
        GpuTexture resource,
        boolean read,
        boolean write
    ) {
    }
}
