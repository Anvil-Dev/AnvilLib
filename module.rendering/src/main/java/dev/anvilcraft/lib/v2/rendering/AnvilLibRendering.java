package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderManager;
import dev.anvilcraft.lib.v2.rendering.gui.renderer.BlockStatePipRenderer;
import dev.anvilcraft.lib.v2.rendering.gui.renderer.StructurePipRenderer;
import dev.anvilcraft.lib.v2.rendering.gui.state.BlockStatePipRenderingState;
import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@Slf4j
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
    public static void on(AddClientReloadListenersEvent event) {
        event.addListener(AnvilLibRendering.location("compute_shader_manager"), ALRComputeShaderManager.INSTANCE);
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().render(
                event.getLevelRenderState().cameraRenderState.cullFrustum,
                false
            );
        }
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterTranslucentFeatures event) {
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().render(
                event.getLevelRenderState().cameraRenderState.cullFrustum,
                true
            );
        }
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterTranslucentParticles event) {
        ALRPostEffects.runBloomDraws(event.getModelViewMatrix());
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterLevel event) {
        try {
            ALRPostEffects.getBloomPostEffect().process();
        } catch (Exception e) {
            log.error("ALRPostEffects BloomPostEffect", e);
        }

    }

    @SubscribeEvent
    public static void on(RegisterPictureInPictureRenderersEvent event) {
        event.register(BlockStatePipRenderingState.class, BlockStatePipRenderer::new);
        event.register(StructurePipRenderingState.class, StructurePipRenderer::new);
    }
}
