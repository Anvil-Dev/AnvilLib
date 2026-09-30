package dev.anvilcraft.lib.v2.rendering.projection;

import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(modid = AnvilLibRendering.MODID, value = Dist.CLIENT)
public final class ProjectionRenderEvents {
    private ProjectionRenderEvents() {
    }

    @SubscribeEvent
    public static void pipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ProjectionRenderTypes.BLOCK_PIPELINE);
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        ProjectionRenderer.invalidateResources();
    }

}
