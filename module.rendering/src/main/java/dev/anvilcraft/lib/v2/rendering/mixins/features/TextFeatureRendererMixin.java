package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(TextFeatureRenderer.class)
public abstract class TextFeatureRendererMixin {
    @ModifyExpressionValue(
        method = "renderTranslucent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getTextSubmits()Ljava/util/List;"
        )
    )
    private List<SubmitNodeStorage.TextSubmit> anvillib$filterTextSubmits(List<SubmitNodeStorage.TextSubmit> submits) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
