package dev.anvilcraft.lib.v2.test;

import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import dev.anvilcraft.lib.v2.test.multiblock.init.LibBlocks;
import dev.anvilcraft.lib.v2.test.multiblock.init.LibItemGroups;
import dev.anvilcraft.lib.v2.test.multiblock.init.LibMultiblockControllers;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibTest.MOD_ID)
public class AnvilLibTest {
    public static final String MOD_ID = "anvillib_test";
    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);
    public static final AnvilLibTestConfig CONFIG = ConfigManager.register(MOD_ID, AnvilLibTestConfig::new);

    public AnvilLibTest(IEventBus bus, ModContainer container) {
        dev.anvilcraft.lib.v2.test.all.TestItemGroups.setupRegistration();
        LibBlocks.init();
        LibItemGroups.init(bus);
        LibMultiblockControllers.init();
        AnvilLibTestDatagen.init();
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
