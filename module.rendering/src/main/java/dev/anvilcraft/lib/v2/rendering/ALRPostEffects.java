package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.bloom.BloomPostEffect;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.joml.Matrix4fc;

public class ALRPostEffects {
    @Getter
    private static BloomPostEffect bloomPostEffect;

    public static void createPostEffects() {
        bloomPostEffect = new BloomPostEffect();
    }

    public static void runBloomDraws(Matrix4fc mvMat){
        Minecraft minecraft = Minecraft.getInstance();
        RenderBuffers renderBuffers = minecraft.renderBuffers();
        FeatureRenderDispatcher frd = new FeatureRenderDispatcher(
            bloomPostEffect.getSubmitNodeStorage(),
            minecraft.getModelManager(),
            renderBuffers.bufferSource(),
            minecraft.getAtlasManager(),
            renderBuffers.outlineBufferSource(),
            renderBuffers.crumblingBufferSource(),
            minecraft.font,
            minecraft.gameRenderer.getGameRenderState()
        );
        bloomPostEffect.runBloomDraws(mvMat, frd);
    }
}
