package dev.anvilcraft.lib.v2.rendering.mixins;

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
        // #P7b: ALRPostEffects.createPostEffects();
        // #P7d: CachedBlockEntityRenderingPipeline.create();
    }

    @Inject(
        method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V",
        at = @At("RETURN")
    )
    private void onUpdateLevel(ClientLevel level, CallbackInfo ci) {
        // #P7d: CachedBlockEntityRenderingPipeline.updateLevel(level);
    }
}
