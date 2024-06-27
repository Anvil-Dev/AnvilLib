package dev.anvilcraft.lib.forge;

import dev.anvilcraft.lib.util.Platform;
import net.minecraftforge.fml.ModList;

public class AnvilLibImpl {
    public static Platform getPlatform() {
        return Platform.FORGE;
    }

    public static boolean isLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }
}
