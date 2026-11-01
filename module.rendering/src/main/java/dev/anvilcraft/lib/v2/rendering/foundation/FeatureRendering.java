package dev.anvilcraft.lib.v2.rendering.foundation;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

public final class FeatureRendering {
    private FeatureRendering() {}

    public static FeatureRenderDispatcher dispatcher(RenderBuffers buffers) {
        var client = Minecraft.getInstance();
        return new FeatureRenderDispatcher(buffers, client.getModelManager(), client.getAtlasManager(),
            client.font, client.gameRenderer.gameRenderState());
    }

    public static void clear(SubmitNodeStorage storage) {
        storage.drainPhases(phase -> phase.sortInto((submit, ordered) -> {}));
    }

    public static void draw(FeatureRenderDispatcher dispatcher, SubmitNodeStorage storage, RenderTarget target) {
        var previousColor = RenderSystem.outputColorTextureOverride;
        var previousDepth = RenderSystem.outputDepthTextureOverride;
        try {
            RenderSystem.outputColorTextureOverride = target.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
            dispatcher.renderAllFeatures(storage);
        } finally {
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
        }
    }
}
