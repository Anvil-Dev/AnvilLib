package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.bloom.BloomPostEffect;
import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
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

@Mod(value = AnvilLibRendering.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
public class AnvilLibRendering {
    public static final boolean DEBUG = System.getProperty("anvillib.rendering.debugMode") != null;
    public static final String MODID = "anvillib_rendering";

    public AnvilLibRendering(IEventBus modBus) {
    }

    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @SubscribeEvent
    public static void on(RenderFrameEvent.Pre event) {
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().runTasks();
        }
        ALRPostEffects.getBloomPostEffect().beginFrame();
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterTranslucentFeatures event) {
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().render(event.getLevelRenderState().cameraRenderState.cullFrustum);
        }
        ALRPostEffects.runBloomDraws(event.getModelViewMatrix());
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterLevel event) {
        ALRPostEffects.getBloomPostEffect().process();
    }
}
