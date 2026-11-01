package dev.anvilcraft.lib.v2.rendering.mixins.features;

import dev.anvilcraft.lib.v2.rendering.ALROptimizations;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.Collection;
import java.util.ArrayList;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cull before preparation so hidden geometry never allocates or uploads vertex data. */
@Mixin(targets = "net.minecraft.client.renderer.feature.FeatureRenderDispatcher$PhaseSubmitGrouper")
public abstract class FeaturePhaseMixin {
    @Inject(method = "accept", at = @At("HEAD"), cancellable = true)
    private void anvillib$filterOccludedFeature(SubmitNode submit, boolean ordered, CallbackInfo ci) {
        var culler = ALROptimizations.getOcclusionCuller();
        if (!culler.isEmpty() && !culler.shouldDraw(submit)) ci.cancel();
    }
    @ModifyVariable(method = "acceptFeatureGroup", at = @At("HEAD"), argsOnly = true)
    private Collection<SubmitNode> anvillib$filterOccludedGroup(Collection<SubmitNode> submits) {
        var culler = ALROptimizations.getOcclusionCuller();
        if (culler.isEmpty()) return submits;
        var visible = new ArrayList<SubmitNode>(submits.size());
        for (SubmitNode submit : submits) if (culler.shouldDraw(submit)) visible.add(submit);
        return visible;
    }
}
