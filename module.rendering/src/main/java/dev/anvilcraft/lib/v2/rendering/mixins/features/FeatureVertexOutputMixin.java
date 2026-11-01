package dev.anvilcraft.lib.v2.rendering.mixins.features;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.foundation.FeatureGeometryCapture;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTypeFeatureRenderer.class)
public abstract class FeatureVertexOutputMixin {
    @Inject(method = "getVertexBuilder", at = @At("HEAD"), cancellable = true)
    private void anvillib$captureGeometry(RenderType type, CallbackInfoReturnable<VertexConsumer> cir) {
        var buffer = FeatureGeometryCapture.getBuffer(type);
        if (buffer != null) cir.setReturnValue(buffer);
    }
}
