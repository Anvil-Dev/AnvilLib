package dev.anvilcraft.lib.v2.cube.client;

import dev.anvilcraft.lib.v2.cube.AnvilLibCube;
import dev.anvilcraft.lib.v2.cube.client.model.ModelSelections;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

@EventBusSubscriber(modid = AnvilLibCube.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CubeModelEvents {
    private static ModelSelections.Snapshot pending = new ModelSelections.Snapshot(Map.of(), 0, 0);
    private CubeModelEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void prepare(ModelEvent.ModifyBakingResult event) {
        pending = ModelSelections.bake(event.getModels());
    }

    @SubscribeEvent
    public static void apply(ModelEvent.BakingCompleted event) {
        CubeSelection.install(pending);
        pending = new ModelSelections.Snapshot(Map.of(), 0, 0);
    }
}
