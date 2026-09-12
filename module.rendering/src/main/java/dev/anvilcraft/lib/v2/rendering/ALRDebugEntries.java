package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.debug.OcclusionCullingDebugEntry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ALRDebugEntries {
    @SubscribeEvent
    public static void on(RegisterDebugEntriesEvent event) {
        event.register(OcclusionCullingDebugEntry.LOCATION, new OcclusionCullingDebugEntry());
    }
}
