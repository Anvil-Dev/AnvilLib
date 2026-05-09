package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.bloom.BloomPostEffect;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.pipeline.RegisterPipelineModifiersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = ALRendering.MODID, dist = Dist.CLIENT)
@EventBusSubscriber
public class ALRendering {
    public static final boolean DEBUG = System.getProperty("anvillib.rendering.debugMode") != null;
    public static final String MODID = "anvillib_rendering";

    private static final Logger logger = LoggerFactory.getLogger("anvillib_rendering");

    @Getter
    private static BloomPostEffect bloomPostEffect;

    public ALRendering(IEventBus modBus) {
    }

    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static void createPipelines() {
        bloomPostEffect = new BloomPostEffect();
        logger.info("Created pipelines");
    }

    @SubscribeEvent
    public static void on(RegisterPipelineModifiersEvent event) {
        event.register(BloomPostEffect.REDIRECT_TO_BLOOM, BloomPostEffect::applyRedirect);
    }

    @SubscribeEvent
    public static void on(RenderFrameEvent.Pre event) {
        bloomPostEffect.beginFrame();
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterLevel event) {
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
        bloomPostEffect.process(event.getModelViewMatrix(), frd);
    }
}
