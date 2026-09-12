package dev.anvilcraft.lib.v2.rendering;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** 字体与轮盘共用的 GUI 渲染支持。 */
@Mod(value = AnvilLibRendering.MODID, dist = Dist.CLIENT)
public class AnvilLibRendering {
    public static final String MODID = "anvillib_rendering";
    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
