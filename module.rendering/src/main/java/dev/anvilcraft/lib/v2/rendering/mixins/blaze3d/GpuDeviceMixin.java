package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRHICapabilities;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ExtendedTextureFormat;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstance;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstanceKey;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query.GpuQueryObject;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.BindlessTexturingSupport;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Supplier;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(GpuDevice.class)
public class GpuDeviceMixin implements ALRGpuDeviceExtension {
    @Shadow
    @Final
    private GpuDeviceBackend backend;

    @Override
    public ALRComputeProgramInstance alrCompileComputePipeline(ALRComputePipeline pipeline, ALRComputeProgramInstanceKey instanceKey) {
        return alrBackend().alrCompileComputePipeline(pipeline, instanceKey);
    }

    @Override
    public void alrDestroyComputeShader(ALRComputeProgramInstance instance) {
        alrBackend().alrDestroyComputeShader(instance);
    }

    @Override
    public GpuQueryObject alrCreateSamplesQuery() {
        return alrBackend().alrCreateSamplesQuery();
    }

    @Override
    public void alrPushDebugGroup(Supplier<String> message) {
        alrBackend().alrPushDebugGroup(message);
    }

    @Override
    public void alrPopDebugGroup() {
        alrBackend().alrPopDebugGroup();
    }

    @Override
    public ALRHICapabilities alrhiCreateCapabilities() {
        return alrBackend().alrhiCreateCapabilities();
    }

    @Override
    public GpuTexture alrCreateExtendedTexture(
        @Nullable String label,
        @GpuTexture.Usage int usage,
        ExtendedTextureFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels
    ){
        return alrBackend().alrCreateExtendedTexture(
            label, usage, format, width, height, depthOrLayers, mipLevels
        );
    }

    @Override
    public int alrGetUniformLocation(ALRComputeProgramInstance program, ALRComputePipeline owner, String name) {
        return alrBackend().alrGetUniformLocation(program, owner, name);
    }

    @Override
    public BindlessTexturingSupport alrGetBindlessTexturingSupport() {
        return alrBackend().alrGetBindlessTexturingSupport();
    }

    @Unique
    private ALRGpuDeviceBackendExtension alrBackend() {
        return ((ALRGpuDeviceBackendExtension) this.backend);
    }
}
