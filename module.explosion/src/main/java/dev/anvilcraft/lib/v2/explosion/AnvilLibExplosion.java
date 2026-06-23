package dev.anvilcraft.lib.v2.explosion;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibExplosion.MOD_ID)
public class AnvilLibExplosion {
    public static final String MOD_ID = "anvillib_explosion";

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibExplosion.MOD_ID, path);
    }
}