package dev.anvilcraft.lib.v2.rendering.integration.mixins;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.RebuildTask;
import dev.anvilcraft.lib.v2.rendering.integration.IrisSupport;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the cached BER compile as a "level" Iris render (so Iris extensions such as extended vertex
 * formats are skipped for the cached geometry) and restores the previous Iris state afterwards.
 * <p>
 * Gated by {@link ALRIntegrationCompatMixinPlugin} on the "iris" mod being loaded.
 */
@Mixin(RebuildTask.class)
@ApiStatus.Internal
public class RebuildTaskMixin {
    @Inject(
        method = "run",
        at = @At("HEAD")
    )
    void handleBegin(CallbackInfo ci) {
        IrisSupport.pushIrisGlobalState();
    }

    @Inject(
        method = "run",
        at = @At("RETURN")
    )
    void handleEnd(CallbackInfo ci) {
        IrisSupport.popIrisGlobalState();
    }
}
