package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.GpuBufferConstants;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlUtil")
public class GlUtilMixin {

    @Inject(
        method = "selectBufferBindTarget",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void handleSSBO(int usage, CallbackInfoReturnable<Integer> cir) {
        if ((usage & GpuBufferConstants.USAGE_SHADER_STORAGE) != 0) {
            cir.setReturnValue(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER);
        }
    }
}
