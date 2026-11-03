package dev.anvilcraft.lib.v2.ui;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibUi.MOD_ID)
public final class AnvilLibUi {
    public static final String MOD_ID = "anvillib_ui";

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(AnvilLibUi.MOD_ID, path);
    }
}
