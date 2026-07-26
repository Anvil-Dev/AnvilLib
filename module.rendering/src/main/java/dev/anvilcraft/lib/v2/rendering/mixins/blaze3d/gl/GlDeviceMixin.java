package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import com.mojang.blaze3d.opengl.GlDebugLabel;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.ALRDebugLabelExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstance;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstanceKey;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderManager;
import org.lwjgl.opengl.ARBComputeShader;
import org.lwjgl.opengl.GL46;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
@org.jetbrains.annotations.ApiStatus.Internal
public abstract class GlDeviceMixin implements ALRGpuDeviceBackendExtension {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public abstract GlDebugLabel debugLabels();

    @Override
    public void alrDestroyComputeShader(ALRComputeProgramInstance instance) {
        GL46.glDeleteProgram(instance.id());
    }

    @Override
    public ALRComputeProgramInstance alrCompileComputeShader(ALRComputeProgramInstanceKey instanceKey) {
        String source = ALRComputeShaderManager.INSTANCE.getSource(instanceKey.location());
        int shaderId = GL46.glCreateShader(ARBComputeShader.GL_COMPUTE_SHADER);
        GL46.glShaderSource(shaderId, source);
        GL46.glCompileShader(shaderId);
        if (GL46.glGetShaderi(shaderId, GL46.GL_COMPILE_STATUS) == 0) {
            String infoLog = GL46.glGetShaderInfoLog(shaderId);
            LOGGER.error("Could not compile COMPUTE shader {}: {}", instanceKey.location(), infoLog);
            return ALRComputeProgramInstance.INVALID;
        }
        int program = GL46.glCreateProgram();
        GL46.glAttachShader(program, shaderId);
        GL46.glLinkProgram(program);
        if (GL46.glGetProgrami(program, GL46.GL_LINK_STATUS) == 0) {
            String infoLog = GL46.glGetProgramInfoLog(shaderId);
            LOGGER.error("Could not link COMPUTE shader {}: {}", instanceKey.location(), infoLog);
            return ALRComputeProgramInstance.INVALID;
        }
        GL46.glDeleteShader(shaderId);
        ALRComputeProgramInstance instance = new ALRComputeProgramInstance(program, instanceKey);
        ((ALRDebugLabelExtension) this.debugLabels()).alrApplyLabel(instance);
        return instance;
    }

    @Override
    public void alrPushDebugGroup(String name) {
        this.debugLabels().pushDebugGroup(() -> name);
    }

    @Override
    public void alrPopDebugGroup() {
        this.debugLabels().popDebugGroup();
    }
}
