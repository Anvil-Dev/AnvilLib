package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderManager;
import dev.anvilcraft.lib.v2.rendering.postprocess.ALRPostEffectShaders;
import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.rendering.sdf.SdfShaders;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
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

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @SubscribeEvent
    public static void on(RegisterShadersEvent event) {
        // #P7b: post-processing shaders (bloom downsample/upsample/apply, blur, glitch)
        ALRPostEffectShaders.register(event);
        // #P7c: SDF GUI shader (sdf_graphics)
        SdfShaders.register(event);
    }

    @SubscribeEvent
    public static void on(RegisterClientReloadListenersEvent event) {
        // #P7e: compute shader manager (compiles and caches GL compute programs)
        event.registerReloadListener(ALRComputeShaderManager.INSTANCE);
    }

    @SubscribeEvent
    public static void on(RenderFrameEvent.Pre event) {
        if (ALRPostEffects.getBloomPostEffect() != null) {
            ALRPostEffects.getBloomPostEffect().beginFrame();
        }
        // #P7c: reset the per-frame SDF parameter slots (the 26.1 GuiRendererMixin flush equivalent).
        SdfGraphics.flush();
        // #P7d: run the cached BER compile + upload queues on the render thread.
        if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
            CachedBlockEntityRenderingPipeline.getInstance().runTasks();
        }
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ALRPostEffects.runBloomDraws(event.getModelViewMatrix());
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            try {
                if (ALRPostEffects.getBloomPostEffect() != null) {
                    ALRPostEffects.getBloomPostEffect().process();
                }
            } catch (Exception e) {
                log.error("ALRPostEffects BloomPostEffect", e);
            }
        }
    }

    /**
     * #P7d: cached BER rendering. Opaque layers render after the opaque world (mapped from the 26.1
     * {@code AfterOpaqueBlocks}), translucent layers after the translucent world (26.1
     * {@code AfterTranslucentFeatures}); both exist as {@link RenderLevelStageEvent.Stage}s on 1.21.1.
     */
    @SubscribeEvent
    public static void onRenderCachedBlockEntities(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
                CachedBlockEntityRenderingPipeline.getInstance().render(event.getFrustum(), false);
            }
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            if (CachedBlockEntityRenderingPipeline.getInstance() != null) {
                CachedBlockEntityRenderingPipeline.getInstance().render(event.getFrustum(), true);
            }
        }
    }
}
