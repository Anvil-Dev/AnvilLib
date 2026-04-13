package dev.anvilcraft.lib.v2.test;

import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.test.multiblock.init.LibMultiblocks;
import dev.anvilcraft.lib.v2.test.wheel.data.lang.LangHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static dev.anvilcraft.lib.v2.test.AnvilLibTest.REGISTRUM;

@EventBusSubscriber(modid = AnvilLibTest.MOD_ID)
public class AnvilLibTestDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
    }

    public static void init() {
        var genInit = REGISTRUM.getDataGenInitializer();
        genInit.add(LibRegistries.DEFINITIONS_KEY, LibMultiblocks::bootstrap);

        REGISTRUM.addDataGenerator(ProviderType.LANG, LangHandler::init);
    }
}
