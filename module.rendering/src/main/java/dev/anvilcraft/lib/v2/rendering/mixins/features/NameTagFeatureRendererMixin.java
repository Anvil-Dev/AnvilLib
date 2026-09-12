package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(NameTagFeatureRenderer.class)
public abstract class NameTagFeatureRendererMixin {
    @ModifyReceiver(
        method = "renderTranslucent",
        at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"),
        expect = 2
    )
    private List<SubmitNodeStorage.NameTagSubmit> anvillib$filterNameTagSubmits(List<SubmitNodeStorage.NameTagSubmit> submits) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
