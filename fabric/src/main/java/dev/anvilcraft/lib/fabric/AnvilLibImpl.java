package dev.anvilcraft.lib.fabric;

import dev.anvilcraft.lib.util.Platform;
import net.fabricmc.loader.api.FabricLoader;

public class AnvilLibImpl {
    public static Platform getPlatform() {
        return Platform.FABRIC;
    }

    public static boolean isLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
}
