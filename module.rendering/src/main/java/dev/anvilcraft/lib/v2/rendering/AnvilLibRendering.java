package dev.anvilcraft.lib.v2.rendering;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** 字体与轮盘共用的 GUI 渲染支持。 */
@Mod(value = AnvilLibRendering.MODID, dist = Dist.CLIENT)
public class AnvilLibRendering {
    public static final String MODID = "anvillib_rendering";
    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
