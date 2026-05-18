package dev.anvilcraft.lib.v2.ui;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibUi.MOD_ID)
public class AnvilLibUi {
    public static final String MOD_ID = "anvillib_ui";

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibUi.MOD_ID, path);
    }
}