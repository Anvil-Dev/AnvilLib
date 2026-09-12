package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(LeashFeatureRenderer.class)
public abstract class LeashFeatureRendererMixin {
    @ModifyExpressionValue(
        method = "renderSolid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getLeashSubmits()Ljava/util/List;"
        )
    )
    private static List<SubmitNodeStorage.LeashSubmit> anvillib$filterLeashSubmits(List<SubmitNodeStorage.LeashSubmit> submits) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
