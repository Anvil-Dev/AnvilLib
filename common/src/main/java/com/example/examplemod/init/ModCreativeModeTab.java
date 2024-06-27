package com.example.examplemod.init;

import com.example.examplemod.api.registry.ExampleModRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;

import static com.example.examplemod.ExampleMod.REGISTRATE;

public class ModCreativeModeTab {
    public static final RegistryEntry<CreativeModeTab> EXAMPLE_TAB = REGISTRATE
            .defaultCreativeTab("example_tab", builder -> builder
                    .displayItems(new RegistrateDisplayItemsGenerator("example_tab", REGISTRATE))
                    .icon(ModItems.EXAMPLE_ITEM::asStack)
                    .build()
            ).register();

    public static void register() {

    }
    public static class RegistrateDisplayItemsGenerator implements CreativeModeTab.DisplayItemsGenerator {

        public final String name;
        public final ExampleModRegistrate registrate;

        public RegistrateDisplayItemsGenerator(String name, ExampleModRegistrate registrate) {
            this.name = name;
            this.registrate = registrate;
        }

        @Override
        public void accept(@Nonnull CreativeModeTab.ItemDisplayParameters itemDisplayParameters, @Nonnull CreativeModeTab.Output output) {
            var tab = registrate.get(name, Registries.CREATIVE_MODE_TAB);
            for (var entry : registrate.getAll(Registries.BLOCK)) {
                if (!registrate.isInCreativeTab(entry, tab))
                    continue;
                Item item = entry.get().asItem();
                if (item == Items.AIR)
                    continue;
                else {
                    output.accept(item);
                }
            }
            for (var entry : registrate.getAll(Registries.ITEM)) {
                if (!registrate.isInCreativeTab(entry, tab))
                    continue;
                Item item = entry.get();
                if (item instanceof BlockItem)
                    continue;
                else {
                    output.accept(item);
                }
            }
        }
    }
}
