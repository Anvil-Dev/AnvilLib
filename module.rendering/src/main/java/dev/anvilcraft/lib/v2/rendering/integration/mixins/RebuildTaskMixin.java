package dev.anvilcraft.lib.v2.rendering.integration.mixins;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.RebuildTask;
import dev.anvilcraft.lib.v2.rendering.integration.IrisSupport;
import net.irisshaders.iris.vertices.ImmediateState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RebuildTask.class)
public class RebuildTaskMixin {
    @Inject(
        method = "run",
        at = @At("HEAD")
    )
    void handleBegin(CallbackInfo ci) {
        IrisSupport.pushIrisGlobalState();
        ImmediateState.isRenderingLevel = true;
        ImmediateState.skipExtension.set(false);
    }

    @Inject(
        method = "run",
        at = @At("RETURN")
    )
    void handleEnd(CallbackInfo ci){
        IrisSupport.popIrisGlobalState();
    }

}
