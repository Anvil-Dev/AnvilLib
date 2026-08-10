package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.bloom.BloomPostEffect;
import dev.anvilcraft.lib.v2.rendering.event.MainTargetResizeEvent;
import dev.anvilcraft.lib.v2.rendering.glitch.GlitchPostEffect;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;

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

    public static void runBloomDraws(Matrix4f mvMat) {
        try {
            bloomPostEffect.runBloomDraws(mvMat);
        } catch (Exception e) {
            log.error("runBloomDraws boom!", e);
        }
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
