package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ShadowFeatureRenderer.class)
public abstract class ShadowFeatureRendererMixin {
    @ModifyExpressionValue(
        method = "renderTranslucent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getShadowSubmits()Ljava/util/List;"
        )
    )
    private List<SubmitNodeStorage.ShadowSubmit> anvillib$filterShadowSubmits(List<SubmitNodeStorage.ShadowSubmit> submits) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
