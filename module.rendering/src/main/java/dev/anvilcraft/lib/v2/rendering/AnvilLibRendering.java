package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.anvilcraft.lib.v2.rendering.event.MainTargetResizeEvent;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderManager;
import dev.anvilcraft.lib.v2.rendering.gui.renderer.BlockStatePipRenderer;
import dev.anvilcraft.lib.v2.rendering.gui.renderer.StructurePipRenderer;
import dev.anvilcraft.lib.v2.rendering.gui.state.BlockStatePipRenderingState;
import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionCuller;
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
import net.neoforged.neoforge.client.event.PrepareRenderBuffersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.ApiStatus;

@Slf4j
@Mod(value = AnvilLibRendering.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
@ApiStatus.Internal
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
        // Loading overlays can render frames before Minecraft construction has completed.
        if (ALRPostEffects.getBloomPostEffect() == null) return;
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().runTasks();
        }
        ALRPostEffects.getBloomPostEffect().beginFrame();
        ALROptimizations.getOcclusionCuller().beginRenderingFrame();
    }

    @SubscribeEvent
    public static void on(AddClientReloadListenersEvent event) {
        event.addListener(AnvilLibRendering.location("compute_shader_manager"), ALRComputeShaderManager.INSTANCE);
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().render(
                false, event.getRenderPass(), null
            );
        }
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterTranslucentFeatures event) {
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().render(
                true, event.getRenderPass(), null
            );
        }
    }

    @SubscribeEvent
    public static void on(MainTargetResizeEvent event) {
        OcclusionCuller occlusionCuller = ALROptimizations.getOcclusionCuller();
        occlusionCuller.onResize(event.getNewWidth(), event.getNewHeight());
    }

    @SubscribeEvent
    public static void on(PrepareRenderBuffersEvent event) {
        var pipeline = CachedBlockEntityRenderingPipeline.getInstance();
        if (pipeline != null) {
            var camera = event.getLevelRenderState().cameraRenderState;
            pipeline.prepare(camera.cullFrustum, camera.pos);
        }
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.OitTranslucent event) {
        var pipeline = CachedBlockEntityRenderingPipeline.getInstance();
        if (pipeline != null) pipeline.render(true, event.getRenderPass(), event.getOitStage());
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterLevel event) {
        ALROptimizations.getOcclusionCuller().processFeatures(event.getLevelRenderState().cameraRenderState);
        ALRPostEffects.runBloomDraws(event.getModelViewMatrix());
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
