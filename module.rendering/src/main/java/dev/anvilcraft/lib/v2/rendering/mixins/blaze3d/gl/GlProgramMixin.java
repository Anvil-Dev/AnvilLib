package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.Uniform;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.NamedUniformAccess;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlProgram.class)
public abstract class GlProgramMixin implements NamedUniformAccess {
    @Shadow
    public abstract @Nullable Uniform getUniform(String name);

    @Override
    public int getUniformLocation(String name, ALRGpuDeviceBackendExtension device) {
        Uniform uniform = getUniform(name);
        if (uniform == null) {
            return INVALID_UNIFORM_LOCATION;
        }
        return switch (uniform) {
            case Uniform.Sampler(int location, _) -> location;
            case Uniform.Ubo(int blockBinding) -> blockBinding;
            case Uniform.Utb(int location, _, _, _) -> location;
        };
    }
}
