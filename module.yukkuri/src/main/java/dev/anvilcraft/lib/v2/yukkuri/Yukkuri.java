package dev.anvilcraft.lib.v2.yukkuri;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Shared identifiers and logging for AnvilLib's Yukkuri vaporization module. */
public final class Yukkuri {
    public static final String MOD_ID = "anvillib_yukkuri";
    public static final String VAPOR_NAMESPACE = "yukkuri";
    public static final Logger LOGGER = LogUtils.getLogger();

    private Yukkuri() {
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(VAPOR_NAMESPACE, path);
    }
}
