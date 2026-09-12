package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ParticleFeatureRenderer.class)
public abstract class ParticleFeatureRendererMixin {
    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getParticleGroupRenderers()Ljava/util/List;"
        )
    )
    private List<SubmitNodeCollector.ParticleGroupRenderer> anvillib$filterParticleGroupRenderers(
        List<SubmitNodeCollector.ParticleGroupRenderer> submits
    ) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
