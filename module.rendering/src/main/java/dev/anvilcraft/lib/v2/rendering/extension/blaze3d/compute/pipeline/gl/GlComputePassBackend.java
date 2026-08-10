package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.BufferSlice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePassBackend;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstance;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.ARBComputeShader;
import org.lwjgl.opengl.ARBShaderAtomicCounters;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.KHRDebug;

/**
 * Standalone GL backend for {@link dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass}.
 * <p>
 * Ported from 26.1 (see #P7e): the 26.1 mixin targets ({@code GpuDevice}/{@code CommandEncoder}/
 * {@code GlDevice}/{@code GlCommandEncoder}/{@code DirectStateAccess}/{@code GlDebugLabel}) do not
 * exist on 1.21.1, so this class owns the program/buffer state machine directly. All GL capability
 * entry points used here are available in the 1.21.1 LWJGL; the program compile/link logic lives in
 * {@link ALRComputeShaderManager}.
 */
@ApiStatus.Internal
public class GlComputePassBackend implements ALRComputePassBackend {
    private static final int DEFAULT_IMAGE_FORMAT = GL11.GL_RGBA8;

    private final Int2ObjectMap<TextureBinding.SamplerAndTexture> textureBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ImageState> imageBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<BufferSlice> uniformBlockBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<BufferSlice> shaderStorageBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<BufferSlice> atomicCounterBindings = new Int2ObjectOpenHashMap<>();
    private ALRComputePipeline pipeline = null;

    public GlComputePassBackend() {
    }

    @Override
    public void setPipeline(ALRComputePipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void pushDebugGroup(String name) {
        if (org.lwjgl.opengl.GL.getCapabilities().GL_KHR_debug) {
            KHRDebug.glPushDebugGroup(KHRDebug.GL_DEBUG_SOURCE_APPLICATION, 0, name);
        }
    }

    @Override
    public void popDebugGroup() {
        if (org.lwjgl.opengl.GL.getCapabilities().GL_KHR_debug) {
            KHRDebug.glPopDebugGroup();
        }
    }

    @Override
    public void dispatchWorkgroups(int groupCountX, int groupCountY, int groupCountZ) {
        this.setupState();
        ARBComputeShader.glDispatchCompute(groupCountX, groupCountY, groupCountZ);
    }

    @Override
    public void dispatchWorkgroupsIndirect(BufferSlice buffer) {
        this.setupState();
        GlStateManager._glBindBuffer(ARBComputeShader.GL_DISPATCH_INDIRECT_BUFFER, buffer.bufferId());
        ARBComputeShader.glDispatchComputeIndirect(buffer.offset());
    }

    @Override
    public void memoryBarrier(MemoryBarrierFlag... flags) {
        ARBShaderImageLoadStore.glMemoryBarrier(MemoryBarrierFlag.compound(flags));
    }

    @Override
    public void bindTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource) {
        this.textureBindings.put(bindingPoint, resource);
    }

    @Override
    public void bindImage(int bindingPoint, int textureId, boolean read, boolean write) {
        this.bindImage(bindingPoint, textureId, read, write, DEFAULT_IMAGE_FORMAT);
    }

    @Override
    public void bindImage(int bindingPoint, int textureId, boolean read, boolean write, int internalFormat) {
        if (!read && !write) {
            throw new IllegalArgumentException("ImageResource does not allow both read and write are false");
        }
        this.imageBindings.put(bindingPoint, new ImageState(textureId, read, write, internalFormat));
    }

    @Override
    public void bindUniformBlock(int bindingPoint, BufferSlice resource) {
        this.uniformBlockBindings.put(bindingPoint, resource);
    }

    @Override
    public void bindShaderStorage(int bindingPoint, BufferSlice resource) {
        this.shaderStorageBindings.put(bindingPoint, resource);
    }

    @Override
    public void bindAtomicCounter(int bindingPoint, BufferSlice resource) {
        this.atomicCounterBindings.put(bindingPoint, resource);
    }

    @Override
    public void close() {

    }

    private void setupState() {
        ALRComputeProgramInstance program = ALRComputeShaderManager.INSTANCE.getShader(pipeline);
        if (program == null || program == ALRComputeProgramInstance.INVALID) return;
        GL20.glUseProgram(program.id());

        for (Int2ObjectMap.Entry<TextureBinding.SamplerAndTexture> entry : this.textureBindings.int2ObjectEntrySet()) {
            this.setupTexture(entry.getIntKey(), entry.getValue());
        }
        for (Int2ObjectMap.Entry<ImageState> entry : this.imageBindings.int2ObjectEntrySet()) {
            this.setupImage(entry.getIntKey(), entry.getValue());
        }
        for (Int2ObjectMap.Entry<BufferSlice> entry : this.uniformBlockBindings.int2ObjectEntrySet()) {
            this.setupUniformBlock(entry.getIntKey(), entry.getValue());
        }
        for (Int2ObjectMap.Entry<BufferSlice> entry : this.shaderStorageBindings.int2ObjectEntrySet()) {
            this.setupShaderStorage(entry.getIntKey(), entry.getValue());
        }
        for (Int2ObjectMap.Entry<BufferSlice> entry : this.atomicCounterBindings.int2ObjectEntrySet()) {
            this.setupAtomicCounter(entry.getIntKey(), entry.getValue());
        }
    }

    private void setupTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource) {
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + bindingPoint);
        GlStateManager._bindTexture(resource.textureId());
        if (resource.samplerId() != 0) {
            GL33.glBindSampler(bindingPoint, resource.samplerId());
        }
    }

    private void setupImage(int bindingPoint, ImageState state) {
        int access = 0;
        if (state.read() && state.write()) {
            access = GL15.GL_READ_WRITE;
        } else {
            if (state.read()) {
                access |= GL15.GL_READ_ONLY;
            }
            if (state.write()) {
                access |= GL15.GL_WRITE_ONLY;
            }
        }

        ARBShaderImageLoadStore.glBindImageTexture(
            bindingPoint,
            state.textureId(),
            0,
            false,
            0,
            access,
            state.internalFormat()
        );
    }

    private void setupUniformBlock(int bindingPoint, BufferSlice resource) {
        GL31.glBindBufferRange(
            GL31.GL_UNIFORM_BUFFER,
            bindingPoint,
            resource.bufferId(),
            resource.offset(),
            resource.length()
        );
    }

    private void setupShaderStorage(int bindingPoint, BufferSlice resource) {
        GL31.glBindBufferRange(
            ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER,
            bindingPoint,
            resource.bufferId(),
            resource.offset(),
            resource.length()
        );
    }

    private void setupAtomicCounter(int bindingPoint, BufferSlice resource) {
        GL31.glBindBufferRange(
            ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER,
            bindingPoint,
            resource.bufferId(),
            resource.offset(),
            resource.length()
        );
    }

    private record ImageState(
        int textureId,
        boolean read,
        boolean write,
        int internalFormat
    ) {
    }
}
