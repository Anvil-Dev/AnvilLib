package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin {
    @ModifyExpressionValue(
        method = {"renderSolid", "renderTranslucent"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getItemSubmits()Ljava/util/List;"
        )
    )
    private List<SubmitNodeStorage.ItemSubmit> anvillib$filterItemSubmits(List<SubmitNodeStorage.ItemSubmit> submits) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
