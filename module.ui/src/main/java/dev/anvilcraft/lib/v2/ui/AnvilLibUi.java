package dev.anvilcraft.lib.v2.ui;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = AnvilLibUi.MODID, dist = Dist.CLIENT)
public final class AnvilLibUi {
    public static final String MODID = "anvillib_ui";

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(AnvilLibUi.MODID, path);
    }
}
