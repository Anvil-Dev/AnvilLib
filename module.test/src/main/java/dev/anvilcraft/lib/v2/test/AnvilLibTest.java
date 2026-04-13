package dev.anvilcraft.lib.v2.test;

import dev.anvilcraft.lib.v2.registrum.Registrum;
import dev.anvilcraft.lib.v2.test.multiblock.init.LibBlocks;
import dev.anvilcraft.lib.v2.test.multiblock.init.LibItemGroups;
import dev.anvilcraft.lib.v2.test.multiblock.init.LibMultiblockControllers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibTest.MOD_ID)
public class AnvilLibTest {
    public static final String MOD_ID = "anvillib_test";
    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);

    public AnvilLibTest(IEventBus bus, ModContainer container) {
        LibBlocks.init();
        LibItemGroups.init(bus);
        LibMultiblockControllers.init();
        AnvilLibTestDatagen.init();
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
