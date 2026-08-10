package dev.anvilcraft.lib.v2.rendering.mixins;

import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
@ApiStatus.Internal
public class MinecraftMixin {

    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onCreateInstance(GameConfig gameConfig, CallbackInfo ci) {
        // #P7b: creates the post-processing effects (bloom/glitch) once the main render target exists.
        ALRPostEffects.createPostEffects();
        // #P7d: initializes the cached BER pipeline (GL debug label support).
        CachedBlockEntityRenderingPipeline.create();
    }

    @Inject(
        method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V",
        at = @At("RETURN")
    )
    private void onUpdateLevel(ClientLevel level, CallbackInfo ci) {
        // #P7d: swap the cached BER pipeline instance when the client level changes.
        CachedBlockEntityRenderingPipeline.updateLevel(level);
    }
}
