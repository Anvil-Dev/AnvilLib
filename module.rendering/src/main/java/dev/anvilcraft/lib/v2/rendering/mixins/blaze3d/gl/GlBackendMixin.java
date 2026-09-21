package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import com.mojang.blaze3d.opengl.GlBackend;
import dev.anvilcraft.lib.v2.rendering.ALROptions;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlBackend.class)
public class GlBackendMixin {

    @Inject(
        method = "setWindowHints",
        at = @At("RETURN")
    )
    void setDebugContext(CallbackInfo ci) {
        if (ALROptions.DEBUG_CONTEXT) {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
        }
    }
}
