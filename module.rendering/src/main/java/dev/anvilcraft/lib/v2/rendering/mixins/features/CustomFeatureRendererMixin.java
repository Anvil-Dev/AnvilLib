package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(CustomFeatureRenderer.class)
public abstract class CustomFeatureRendererMixin {
    @ModifyReceiver(
        method = {"renderSolid", "renderTranslucent"},
        at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;")
    )
    private List<SubmitNodeStorage.CustomGeometrySubmit> anvillib$filterCustomGeometrySubmits(
        List<SubmitNodeStorage.CustomGeometrySubmit> submits
    ) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
