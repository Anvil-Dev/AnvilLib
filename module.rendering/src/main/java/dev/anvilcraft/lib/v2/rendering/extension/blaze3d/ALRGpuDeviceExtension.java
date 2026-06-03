package dev.anvilcraft.lib.v2.rendering.extension.blaze3d;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderInstance;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderInstanceKey;

public interface ALRGpuDeviceExtension {
    ALRComputeShaderInstance alrCompileComputeShader(ALRComputeShaderInstanceKey instanceKey);

    ALRComputePass alrCreateComputePass();
}
