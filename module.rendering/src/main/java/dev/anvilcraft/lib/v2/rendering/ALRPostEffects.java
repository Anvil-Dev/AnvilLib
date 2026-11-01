package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.bloom.BloomPostEffect;
import dev.anvilcraft.lib.v2.rendering.foundation.FeatureRendering;
import dev.anvilcraft.lib.v2.rendering.event.MainTargetResizeEvent;
import dev.anvilcraft.lib.v2.rendering.glitch.GlitchPostEffect;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4fc;

@Slf4j
@EventBusSubscriber
public class ALRPostEffects {
    @Getter
    private static BloomPostEffect bloomPostEffect;
    @Getter
    private static GlitchPostEffect glitchPostEffect;

    private static RenderBuffers bloomBuffers;
    private static FeatureRenderDispatcher bloomDispatcher;

    public static void createPostEffects() {
        bloomPostEffect = new BloomPostEffect();
        glitchPostEffect = new GlitchPostEffect();
        bloomBuffers = new RenderBuffers(1);
        bloomDispatcher = FeatureRendering.dispatcher(bloomBuffers);
    }

    public static void runBloomDraws(Matrix4fc mvMat) {
        try {
            bloomPostEffect.runBloomDraws(mvMat, bloomDispatcher);
        } finally {
            bloomBuffers.endFrame();
        }
    }

    public static void close() {
        if (bloomDispatcher != null) bloomDispatcher.close();
        if (bloomBuffers != null) bloomBuffers.close();
        if (bloomPostEffect != null) bloomPostEffect.close();
        if (glitchPostEffect != null) glitchPostEffect.close();
    }

    @ApiStatus.Internal
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
