package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ModelPartFeatureRenderer.class)
public abstract class ModelPartFeatureRendererMixin {
    @ModifyReceiver(
        method = "render",
        at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;")
    )
    private List<SubmitNodeStorage.ModelPartSubmit> anvillib$filterModelPartSubmits(List<SubmitNodeStorage.ModelPartSubmit> submits) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
