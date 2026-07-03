package dev.anvilcraft.lib.v2.rendering.integration;

import net.irisshaders.iris.api.v0.IrisApi;
import net.neoforged.fml.ModList;

public class IrisSupport {
    private static final boolean IRIS_PRESENT;

    static {
        IRIS_PRESENT = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
    }

    public static boolean isIrisPresent() {
        return IRIS_PRESENT;
    }

    public static boolean isShaderEnabled() {
        if (IRIS_PRESENT) {
            return isShaderEnabledInternal();
        }
        return false;
    }

    private static boolean isShaderEnabledInternal() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
