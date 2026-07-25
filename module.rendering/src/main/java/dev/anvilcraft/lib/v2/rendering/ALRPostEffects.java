package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.bloom.BloomPostEffect;
import dev.anvilcraft.lib.v2.rendering.event.MainTargetResizeEvent;
import dev.anvilcraft.lib.v2.rendering.glitch.GlitchPostEffect;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4fc;

@Slf4j
@EventBusSubscriber
public class ALRPostEffects {
    @Getter
    private static BloomPostEffect bloomPostEffect;
    @Getter
    private static GlitchPostEffect glitchPostEffect;

    public static void createPostEffects() {
        bloomPostEffect = new BloomPostEffect();
        glitchPostEffect = new GlitchPostEffect();
    }

    public static void runBloomDraws(Matrix4fc mvMat) {
        try {
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
        } catch (Exception e) {
            log.error("runBloomDraws boom!", e);
        }
    }

    @SubscribeEvent
    public static void on(MainTargetResizeEvent event) {
        BloomPostEffect bloomPostEffect = ALRPostEffects.getBloomPostEffect();
        if (bloomPostEffect != null) {
            bloomPostEffect.resize(
                event.getNewWidth(),
                event.getNewHeight()
            );
        }
    }
}
