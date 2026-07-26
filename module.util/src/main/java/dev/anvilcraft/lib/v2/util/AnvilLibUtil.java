package dev.anvilcraft.lib.v2.util;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;

@Mod(AnvilLibUtil.MOD_ID)
public class AnvilLibUtil {
    @ApiStatus.Internal
    public AnvilLibUtil() {
    }

    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_util";

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibUtil.MAIN_ID, path);
    }
}
