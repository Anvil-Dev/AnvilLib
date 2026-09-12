package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionFeatureRendererHelper;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(BlockFeatureRenderer.class)
public abstract class BlockFeatureRendererMixin {
    @ModifyExpressionValue(
        method = "renderMovingBlockSubmits",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getMovingBlockSubmits()Ljava/util/List;"
        )
    )
    private List<SubmitNodeStorage.MovingBlockSubmit> anvillib$filterMovingBlockSubmits(List<SubmitNodeStorage.MovingBlockSubmit> submits) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }

    @ModifyExpressionValue(
        method = "renderBlockModelSubmits",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getBlockModelSubmits()Ljava/util/List;"
        )
    )
    private List<SubmitNodeStorage.BlockModelSubmit> anvillib$filterBlockModelSubmits(List<SubmitNodeStorage.BlockModelSubmit> submits) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }

    @ModifyExpressionValue(
        method = "renderBreakingBlockModelSubmits",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;getBreakingBlockModelSubmits()Ljava/util/List;"
        )
    )
    private List<SubmitNodeStorage.BreakingBlockModelSubmit> anvillib$filterBreakingBlockModelSubmits(
        List<SubmitNodeStorage.BreakingBlockModelSubmit> submits
    ) {
        return OcclusionFeatureRendererHelper.filterVisibleFeatures(submits);
    }
}
