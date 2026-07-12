package dev.anvilcraft.lib.v2.network;

import dev.anvilcraft.lib.v2.config.ConfigManager;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibNetwork.MOD_ID)
public class AnvilLibNetwork {
    public static final String MOD_ID = "anvillib_network";
    public static final AnvilLibNetworkServerConfig CONFIG = ConfigManager.register(
        AnvilLibNetwork.MOD_ID,
        AnvilLibNetworkServerConfig::new
    );
}
