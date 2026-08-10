package dev.anvilcraft.lib.v2.rendering.extension.blaze3d;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstance;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstanceKey;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ALRGpuDeviceBackendExtension {
    ALRComputeProgramInstance alrCompileComputeShader(ALRComputeProgramInstanceKey instanceKey);

    void alrDestroyComputeShader(ALRComputeProgramInstance instance);

    void alrPushDebugGroup(String name);

    void alrPopDebugGroup();
}
