package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d;

import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.api.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.*;
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
@Mixin(FrontendGpuDevice.class)
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
        if (this.backend instanceof ALRGpuDeviceBackendExtension extension) extension.alrPushDebugGroup(message);
    }

    @Override
    public void alrPopDebugGroup() {
        if (this.backend instanceof ALRGpuDeviceBackendExtension extension) extension.alrPopDebugGroup();
    }

    @Override
    public ALRHICapabilities alrhiCreateCapabilities() {
        return this.backend instanceof ALRGpuDeviceBackendExtension extension ? extension.alrhiCreateCapabilities()
            : new ALRHICapabilities(false, false, false, false, 0, new int[3]);
    }

    @Override
    public ALRHIHeuristics alrhiCreateHeuristics() {
        return this.backend instanceof ALRGpuDeviceBackendExtension extension ? extension.alrhiCreateHeuristics() : new ALRHIHeuristics(false);
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
