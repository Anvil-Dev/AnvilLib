package dev.anvilcraft.lib.v2.test;

import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.test.all.TestBlocks;
import dev.anvilcraft.lib.v2.test.all.TestItemGroups;
import dev.anvilcraft.lib.v2.test.all.TestItems;
import dev.anvilcraft.lib.v2.test.all.TestTiles;
import dev.anvilcraft.lib.v2.test.data.TestLangGenerator;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibTest.MOD_ID)
public class AnvilLibTest {
    public static final String MOD_ID = "anvillib_test";
    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);
    public static final AnvilLibTestConfig CONFIG = ConfigManager.register(MOD_ID, AnvilLibTestConfig::new);

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibTest.MOD_ID, path);
    }

    public AnvilLibTest(IEventBus modBus) {
        TestItemGroups.setupRegistration();
        TestBlocks.setupRegistration();
        TestTiles.setupRegistration();
        TestItems.setupRegistration();

        setupDataGeneration();
    }

    public void setupDataGeneration(){
        REGISTRUM.addDataGenerator(ProviderType.LANG, TestLangGenerator::accept);
    }
}
