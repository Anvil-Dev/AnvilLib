package dev.anvilcraft.lib.v2.ui;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = AnvilLibUi.MODID, dist = Dist.CLIENT)
public final class AnvilLibUi {
    public static final String MODID = "anvillib_ui";

    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibUi.MODID, path);
    }
}
