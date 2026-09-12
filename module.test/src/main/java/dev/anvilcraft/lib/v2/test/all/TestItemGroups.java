package dev.anvilcraft.lib.v2.test.all;

import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.world.item.CreativeModeTab;

public class TestItemGroups {
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TEST_TAB = createTab();

    private static RegistryEntry<CreativeModeTab, CreativeModeTab> createTab() {
        var builder = AnvilLibTest.REGISTRUM.creativeTab("test_tab", () -> net.minecraft.world.item.Items.DIAMOND);
        if (Boolean.getBoolean("anvillib.portSmoke")) {
            builder.sectionedDisplayItems(sections -> {
                var banner = dev.anvilcraft.lib.v2.registrum.util.CreativeTabSection.builder(
                    net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/white_concrete.png"))
                    .text(net.minecraft.network.chat.Component.literal("Colors"))
                    .textBackgroundColor(0xCC202020).build();
                sections.section(banner, colors -> {
                    for (var color : dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry.colorOrder()) {
                        colors.accept(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                            net.minecraft.resources.Identifier.withDefaultNamespace(color.getName() + "_wool")));
                    }
                });
                sections.section(banner, items -> items.accept(net.minecraft.world.item.Items.DIAMOND));
            });
        }
        return builder.register();
    }

    public static void setupRegistration() {
    }
}
