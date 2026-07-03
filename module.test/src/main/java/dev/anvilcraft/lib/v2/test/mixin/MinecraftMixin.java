package dev.anvilcraft.lib.v2.test.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import dev.anvilcraft.lib.v2.rendering.bloom.BloomPostEffect;
import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.ALRComputeCapabilities;
import dev.anvilcraft.lib.v2.test.client.compute.ComputeSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    @Final
    private Window window;

    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onCreateInstance(GameConfig gameConfig, CallbackInfo ci) {
        ComputeSupport.init();
    }

}
