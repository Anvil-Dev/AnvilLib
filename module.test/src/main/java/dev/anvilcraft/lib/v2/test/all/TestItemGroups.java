package dev.anvilcraft.lib.v2.test.all;

import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.world.item.CreativeModeTab;

public class TestItemGroups {
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TEST_TAB = AnvilLibTest.REGISTRUM
        .creativeTab("test_tab", () -> TestItems.TEST_ITEM)
        .register();

    public static void setupRegistration() {
    }
}
