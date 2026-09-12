package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.BindlessTexturingSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlTexture.class)
public abstract class GlTextureMixin {

    @Inject(method = "close", at = @At("HEAD"))
    private void alr$notifyTextureDisposed(CallbackInfo ci) {
        ALRGpuDeviceExtension backend = (ALRGpuDeviceExtension) RenderSystem.getDevice();
        BindlessTexturingSupport support = backend.alrGetBindlessTexturingSupport();
        if (support != null) {
            support.alrTextureDisposed((GlTexture) (Object) this);
        }
    }
}
