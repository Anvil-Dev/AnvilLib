package dev.anvilcraft.lib.v2.wheel;

import net.minecraft.resources.ResourceLocation;

public class AnvilLibWheel {
    public static final String MOD_ID = "anvillib_wheel";
    public static final String MAIN_ID = "anvillib";

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(AnvilLibWheel.MAIN_ID, path);
    }
}
