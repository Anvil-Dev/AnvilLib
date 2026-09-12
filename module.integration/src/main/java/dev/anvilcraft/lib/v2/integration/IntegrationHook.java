package dev.anvilcraft.lib.v2.integration;

import lombok.Getter;
import lombok.Setter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class IntegrationHook {
    @Getter
    @Setter
    private static GatherDataEvent.Client event = null;
    @Getter
    @Setter
    private static IEventBus modEventBus = null;
    @Getter
    @Setter
    private static ModContainer modContainer = null;
}
