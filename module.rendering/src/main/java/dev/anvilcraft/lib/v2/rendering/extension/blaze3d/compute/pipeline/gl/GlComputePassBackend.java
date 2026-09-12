package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.gl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePassBackend;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.BindlessImageArrayBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ComputeBindingLayout;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstance;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderManager;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.ExtendedGpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.BindlessTexturingSupport;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.TextureHandle;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl.GlExtendedTextureConstants;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl.bindless.GlTextureHandle;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.ARBShaderAtomicCounters;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL46;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@ApiStatus.Internal
public class GlComputePassBackend implements ALRComputePassBackend {
    private final ALRGpuDeviceBackendExtension backendExtension;
    private final ALRCommandEncoderBackendExtension commandEncoderExtension;
    private final Int2ObjectMap<TextureBinding.SamplerAndTexture> textureBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ImageState> imageBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<GpuBufferSlice> uniformBlockBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<GpuBufferSlice> shaderStorageBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<GpuBufferSlice> atomicCounterBindings = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ImageArrayState> arrayOfImageBindings = new Int2ObjectOpenHashMap<>();
    private final List<BindlessImageArrayState> bindlessImageArrayBindings = new ArrayList<>();
    private final Map<TextureHandle, ResidentState> residentHandles = new HashMap<>();

    private BindlessTexturingSupport residentSupport;
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
    public void pushDebugGroup(Supplier<String> message) {
        this.backendExtension.alrPushDebugGroup(message);
    }

    @Override
    public void popDebugGroup() {
        this.backendExtension.alrPopDebugGroup();
    }

    @Override
    public void dispatchWorkgroups(int groupCountX, int groupCountY, int groupCountZ) {
        try {
            this.setupState();
            this.commandEncoderExtension.alrDispatchWorkgroups(groupCountX, groupCountY, groupCountZ);
        } finally {
            this.releaseResidentHandles();
        }
    }

    @Override
    public void dispatchWorkgroupsIndirect(GpuBufferSlice buffer) {
        try {
            this.setupState();
            this.commandEncoderExtension.alrDispatchWorkgroupsIndirect(buffer);
        } finally {
            this.releaseResidentHandles();
        }
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
    public void bindBindlessImageArray(BindlessImageArrayBinding binding, List<GpuTexture> textures) {
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(textures, "textures");
        if (textures.size() > binding.size()) {
            throw new IllegalArgumentException("views must not have elements more than binding.size");
        }

        BindlessImageArrayState state = BindlessImageArrayState.fromTextures(binding, textures);
        this.bindlessImageArrayBindings.add(state);
    }

    @Override
    public void bindBindlessImageArray(String name, List<GpuTexture> textures) {
        ComputeBindingLayout<?> layout = this.pipeline == null ? null : this.pipeline.getBinding(name);
        if (!(layout instanceof BindlessImageArrayBinding binding)) {
            throw new IllegalArgumentException("pipeline binding is not a bindless image array: " + name);
        }
        this.bindBindlessImageArray(binding, textures);
    }

    @Override
    public void close() {
        RuntimeException failure = null;
        try {
            this.releaseResidentHandles();
        } catch (RuntimeException ex) {
            failure = ex;
        }
        this.textureBindings.clear();
        this.imageBindings.clear();
        this.uniformBlockBindings.clear();
        this.shaderStorageBindings.clear();
        this.atomicCounterBindings.clear();
        this.bindlessImageArrayBindings.clear();
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public void bindArrayOfTexture(int bindingPoint, List<GpuTexture> resource, boolean read, boolean write) {
        this.arrayOfImageBindings.put(bindingPoint, new ImageArrayState(resource, read, write));
    }

    private void setupState() {
        ALRComputeProgramInstance program = ALRComputeShaderManager.INSTANCE.getShader(pipeline);
        if (program == null || program == ALRComputeProgramInstance.INVALID) {
            throw new IllegalStateException("dispatching compute using a invalid program");
        }
        GlStateManager._glUseProgram(program.id());

        for (Int2ObjectMap.Entry<TextureBinding.SamplerAndTexture> entry : this.textureBindings.int2ObjectEntrySet()) {
            this.setupTexture(entry.getIntKey(), entry.getValue(), program);
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
        for (Int2ObjectMap.Entry<ImageArrayState> entry : this.arrayOfImageBindings.int2ObjectEntrySet()) {
            this.setupArrayOfImage(entry.getIntKey(), entry.getValue());
        }
        this.setupBindlessImageArrays(program);
    }

    //TODO test on amd
    private void setupArrayOfImage(int bindingPointStart, ImageArrayState state) {
        for (GpuTexture texture : state.resource) {
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
            int unit = bindingPointStart++;
            if (texture instanceof ExtendedGpuTexture ext) {
                ARBShaderImageLoadStore.glBindImageTexture(
                    unit,
                    ((GlTexture) texture).glId(),
                    0,
                    false,
                    0,
                    access,
                    GlExtendedTextureConstants.toGlInternalId(ext.getActualFormat())
                );
            } else {
                ARBShaderImageLoadStore.glBindImageTexture(
                    unit,
                    ((GlTexture) texture).glId(),
                    0,
                    false,
                    0,
                    access,
                    GlConst.toGlInternalId(texture.getFormat())
                );
            }
        }
    }

    private void setupBindlessImageArrays(ALRComputeProgramInstance program) {
        if (this.bindlessImageArrayBindings.isEmpty()) {
            return;
        }

        BindlessTexturingSupport support = this.backendExtension.alrGetBindlessTexturingSupport();
        if (support == null) {
            throw new IllegalStateException("bindless image arrays require ARB_bindless_texture");
        }
        this.residentSupport = support;

        for (BindlessImageArrayState state : this.bindlessImageArrayBindings) {
            List<TextureHandle> handles = new ArrayList<>(state.views().size());
            for (int index = 0; index < state.views().size(); index++) {
                BindlessImageViewState view = state.views().get(index);
                TextureHandle handle = support.alrCreateImageHandle(
                    view.texture(),
                    view.level(),
                    view.layered(),
                    view.layer()
                );
//                GlTexture texture = (GlTexture) view.texture();
//                long handleId = ((GlTextureHandle) handle).handleId();
//                String format = view.texture() instanceof ExtendedGpuTexture extendedTexture
//                    ? extendedTexture.getActualFormat().name()
//                    : view.texture().getFormat().name();
//                System.out.println(
//                    "ALR bindless image " + state.binding().name() + "[" + index + "]"
//                        + ": label=\"" + view.texture().getLabel() + "\""
//                        + ", glTexture=" + texture.glId()
//                        + ", size=" + view.texture().getWidth(view.level()) + "x" + view.texture().getHeight(view.level())
//                        + ", format=" + format
//                        + ", level=" + view.level()
//                        + ", layered=" + view.layered()
//                        + ", layer=" + view.layer()
//                        + ", handle=" + Long.toUnsignedString(handleId)
//                        + " (0x" + Long.toUnsignedString(handleId, 16) + ")"
//                );
                this.ensureResident(handle, state.binding().write(), state.binding().read());
                handles.add(handle);
            }
            support.alrBindTextureHandleMultiple(program, state.binding().name(), handles);
        }
    }

    private void ensureResident(TextureHandle handle, boolean write, boolean read) {
        ResidentState state = this.residentHandles.get(handle);
        if (state == null) {
            this.residentSupport.alrTextureHandleMakeResident(handle, write, read);
            this.residentHandles.put(handle, new ResidentState(write, read, 1));
            return;
        }

        boolean mergedWrite = state.write() || write;
        boolean mergedRead = state.read() || read;
        if (mergedWrite != state.write() || mergedRead != state.read()) {
            this.residentSupport.alrTextureHandleDeleteResident(handle);
            this.residentSupport.alrTextureHandleMakeResident(handle, mergedWrite, mergedRead);
            state.setWrite(mergedWrite);
            state.setRead(mergedRead);
        }
        state.incrementReferences();
    }

    private void releaseResidentHandles() {
        if (this.residentHandles.isEmpty()) {
            this.residentSupport = null;
            return;
        }

        RuntimeException failure = null;
        for (TextureHandle handle : this.residentHandles.keySet()) {
            try {
                this.residentSupport.alrTextureHandleDeleteResident(handle);
            } catch (RuntimeException ex) {
                if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        this.residentHandles.clear();
        this.residentSupport = null;
        if (failure != null) {
            throw failure;
        }
    }

    private void setupTexture(int bindingPoint, TextureBinding.SamplerAndTexture resource, ALRComputeProgramInstance programInstance) {
        int sampler = ((GlSampler) resource.sampler()).getId();
        int texture = ((GlTexture) resource.texture()).glId();

//        List<ComputeBindingLayout<?>> binding = programInstance.getOwner().getBinding(ShaderResourceType.TEXTURE_OR_IMAGE);
//        if (binding == null) {
//            throw new IllegalArgumentException("texture or image resource is not required by shader " + programInstance.key());
//        }
//        String name = binding.get(bindingPoint).name();
//        int uniformLocation = programInstance.getUniformLocation(name, backendExtension);
//
//        GlStateManager._glUniform1i(uniformLocation, bindingPoint);
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

        if (state.resource() instanceof ExtendedGpuTexture ext) {
            ARBShaderImageLoadStore.glBindImageTexture(
                bindingPoint,
                ((GlTexture) state.resource()).glId(),
                0,
                false,
                0,
                access,
                GlExtendedTextureConstants.toGlInternalId(ext.getActualFormat())
            );
        } else {
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

    private record ImageArrayState(
        List<GpuTexture> resource,
        boolean read,
        boolean write
    ) {
    }

    private record BindlessImageArrayState(
        BindlessImageArrayBinding binding,
        List<BindlessImageViewState> views
    ) {
        private BindlessImageArrayState {
            views = List.copyOf(views);
        }

        private static BindlessImageArrayState fromTextures(
            BindlessImageArrayBinding binding,
            List<GpuTexture> textures
        ) {
            List<BindlessImageViewState> list = new ArrayList<>();
            for (GpuTexture texture : textures) {
                BindlessImageViewState bindlessImageViewState = BindlessImageViewState.of(texture);
                list.add(bindlessImageViewState);
            }
            return new BindlessImageArrayState(
                binding,
                list
            );
        }
    }

    private record BindlessImageViewState(
        GpuTexture texture,
        int level,
        boolean layered,
        int layer
    ) {
        private static BindlessImageViewState of(GpuTexture texture) {
            return new BindlessImageViewState(Objects.requireNonNull(texture, "texture"), 0, false, 0);
        }
    }

    private static final class ResidentState {
        private boolean write;
        private boolean read;
        private int references;

        private ResidentState(boolean write, boolean read, int references) {
            this.write = write;
            this.read = read;
            this.references = references;
        }

        private boolean write() {
            return this.write;
        }

        private boolean read() {
            return this.read;
        }

        private void setWrite(boolean write) {
            this.write = write;
        }

        private void setRead(boolean read) {
            this.read = read;
        }

        private void incrementReferences() {
            this.references++;
        }
    }
}
