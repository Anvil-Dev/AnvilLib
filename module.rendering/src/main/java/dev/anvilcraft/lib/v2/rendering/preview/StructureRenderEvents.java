package dev.anvilcraft.lib.v2.rendering.preview;

import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import dev.anvilcraft.lib.v2.rendering.preview.level.SandboxRenderLevel;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** 注册共享预览渲染器，并修正沙盒内下落方块的光照来源。 */
@EventBusSubscriber(modid = AnvilLibRendering.MODID, value = Dist.CLIENT)
public final class StructureRenderEvents {
    private StructureRenderEvents() {
    }

    @SubscribeEvent
    public static void previews(RegisterPictureInPictureRenderersEvent event) {
        event.register(StructurePreviewPipRenderer.State.class, StructurePreviewPipRenderer::new);
    }

    @SubscribeEvent
    public static void fallingBlocks(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(FallingBlockRenderer.class, (entity, state) -> {
            if (entity.level() instanceof SandboxRenderLevel level) {
                var moving = state.movingBlockRenderState;
                moving.biome = level.getBiome(moving.blockPos);
                moving.cardinalLighting = level.cardinalLighting();
                moving.lightEngine = level.getLightEngine();
            }
        });
    }
}
