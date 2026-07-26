package dev.anvilcraft.lib.v2.rendering.mixins;

import dev.anvilcraft.lib.v2.rendering.event.MainTargetResizeEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.fml.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
@org.jetbrains.annotations.ApiStatus.Internal
public class GameRendererMixin {

    @Inject(
        method = "resize",
        at = @At("HEAD")
    )
    void onResize(int width, int height, CallbackInfo ci) {
        ModLoader.postEvent(new MainTargetResizeEvent(width, height));
    }
}
