package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ModelFeatureRenderer.class)
public abstract class ModelFeatureRendererMixin {
    @ModifyReceiver(
        method = "renderTranslucents",
        at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;")
    )
    private List<SubmitNodeStorage.TranslucentModelSubmit<?>> anvillib$filterTranslucentModelSubmits(
        List<SubmitNodeStorage.TranslucentModelSubmit<?>> submits
    ) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }

    @ModifyReceiver(
        method = "renderBatch",
        at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;")
    )
    private List<SubmitNodeStorage.ModelSubmit<?>> anvillib$filterModelSubmits(List<SubmitNodeStorage.ModelSubmit<?>> submits) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
