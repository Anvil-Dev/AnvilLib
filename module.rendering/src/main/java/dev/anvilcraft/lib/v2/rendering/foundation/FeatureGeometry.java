package dev.anvilcraft.lib.v2.rendering.foundation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.geometry.BakedQuad;

/** Takes a snapshot of mutable tessellation data before submitting deferred geometry. */
public final class FeatureGeometry {
    private FeatureGeometry() {}

    public static void submitQuad(SubmitNodeCollector collector, PoseStack pose, BakedQuad quad, QuadInstance instance) {
        QuadInstance copy = new QuadInstance();
        copy.setOverlayCoords(instance.overlayCoords());
        for (int i = 0; i < 4; i++) {
            copy.setColor(i, instance.getColor(i));
            copy.setLightCoords(i, instance.getLightCoords(i));
        }
        collector.submitCustomGeometry(pose, quad.materialInfo().itemRenderType(),
            (transform, buffer) -> buffer.putBakedQuad(transform, quad, copy));
    }
}
