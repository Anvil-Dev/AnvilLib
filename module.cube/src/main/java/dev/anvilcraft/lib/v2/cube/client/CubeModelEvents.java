package dev.anvilcraft.lib.v2.cube.client;

import dev.anvilcraft.lib.v2.cube.AnvilLibCube;
import dev.anvilcraft.lib.v2.cube.client.model.ModelSelections;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

@EventBusSubscriber(modid = AnvilLibCube.MOD_ID, value = Dist.CLIENT)
public final class CubeModelEvents {
    private static CubeSelectionRules pendingRules = CubeSelectionRules.EMPTY;
    private static ModelSelections.Snapshot pending = new ModelSelections.Snapshot(Map.of(), 0, 0);
    private CubeModelEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void prepare(ModelEvent.ModifyBakingResult event) {
        CubeSelectionRules loaded = CubeSelectionRules.load(Minecraft.getInstance().getResourceManager());
        pending = ModelSelections.bake(event.getBakingResult().blockStateModels(), block -> CubeSelection.isEnabled(block, loaded));
        pendingRules = loaded;
    }

    @SubscribeEvent
    public static void apply(ModelEvent.BakingCompleted event) {
        CubeSelection.install(pending, pendingRules);
        pendingRules = CubeSelectionRules.EMPTY;
        pending = new ModelSelections.Snapshot(Map.of(), 0, 0);
    }
}
