package dev.anvilcraft.lib.v2.multiblock;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibDynamicMultiblock.MOD_ID)
public class AnvilLibDynamicMultiblock {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_dynamic_multiblock";

    public AnvilLibDynamicMultiblock(IEventBus modEventBus, ModContainer modContainer) {
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MAIN_ID, path);
    }
}
