package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.neoforged.neoforge.client.submit.ExtendedBlockFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ExtendedBlockFeatureRenderer.class)
public abstract class ExtendedBlockFeatureRendererMixin {
    @ModifyExpressionValue(
        method = "renderMultiLayerBlockModelSubmits",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getMultiLayerBlockModelSubmits()Ljava/util/List;"
        )
    )
    private static List<SubmitNodeStorage.MultiLayerBlockModelSubmit> anvillib$filterMultiLayerBlockModelSubmits(
        List<SubmitNodeStorage.MultiLayerBlockModelSubmit> submits
    ) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
