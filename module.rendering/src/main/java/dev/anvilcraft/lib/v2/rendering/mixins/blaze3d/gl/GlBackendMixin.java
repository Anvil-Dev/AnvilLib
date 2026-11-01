package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import com.mojang.renderpearl.backend.opengl.GlBackend;
import dev.anvilcraft.lib.v2.rendering.ALROptions;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GlBackend.class)
public class GlBackendMixin {
    // 保留原有上下文标志，并在 SDL 创建窗口前启用调试上下文。
    @ModifyArg(method = "createWindow", at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SetAttribute(II)Z", ordinal = 3), index = 1)
    private int anvillib$debugContextFlags(int flags) {
        return ALROptions.DEBUG_CONTEXT ? flags | SDLVideo.SDL_GL_CONTEXT_DEBUG_FLAG : flags;
    }
}
