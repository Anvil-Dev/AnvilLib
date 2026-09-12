package dev.anvilcraft.lib.v2.test.multiblock.init;

import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.lib.v2.test.AnvilLibTest.REGISTRUM;

public class LibItemGroups {
    private static final DeferredRegister<CreativeModeTab> DF =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnvilLibTest.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TEST_TAB =
        DF.register("tab", () -> CreativeModeTab.builder()
            .icon(LibBlocks.TEST_CONTROLLER::asStack)
            .displayItems((ctx, entries) -> {
            })
            .title(REGISTRUM.addLang("itemGroup", AnvilLibTest.of("tab"), "Test Tab"))
            .build());

    public static void init(IEventBus bus) {
        DF.register(bus);
    }
}
