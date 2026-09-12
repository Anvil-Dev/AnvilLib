package dev.anvilcraft.lib.v2.rendering.integration.mixins;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedRenderingChunk;
import dev.anvilcraft.lib.v2.rendering.integration.IrisSupport;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CachedRenderingChunk.class)
public class CachedChunkMixin {
    @Inject(
        method = "modifyRenderTypeIfNeeded",
        at = @At("HEAD"),
        cancellable = true
    )
    void unwrapIrisRenderType(RenderType rt, CallbackInfoReturnable<RenderType> cir) {
        cir.setReturnValue(IrisSupport.unwrapRenderType(rt));
    }
}
