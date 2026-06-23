package dev.anvilcraft.lib.v2.explosion;

import dev.anvilcraft.lib.v2.config.ConfigManager;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibExplosion.MOD_ID)
public class AnvilLibExplosion {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_explosion";
    public static final AnvilLibExplosionConfig CONFIG = ConfigManager.register(MOD_ID, AnvilLibExplosionConfig::new);

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibExplosion.MAIN_ID, path);
    }
}