package dev.anvilcraft.lib.v2.rendering.integration.mixins;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.anvilcraft.lib.v2.rendering.integration.IrisSupport;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CachedBlockEntityRenderingPipeline.class)
@ApiStatus.Internal
public abstract class CachedBlockEntityRenderingPipelineMixin {

    @Shadow
    public abstract void forcedUpdate();

    @Unique
    private boolean previousShaderEnabled = false;

    @Inject(
        method = "handleIntegration",
        at = @At("HEAD")
    )
    void handleIris(CallbackInfo ci) {
        boolean shaderEnabled = IrisSupport.isShaderEnabled();

        if (previousShaderEnabled != shaderEnabled) {
            this.forcedUpdate();
        }

        this.previousShaderEnabled = shaderEnabled;
    }
}
