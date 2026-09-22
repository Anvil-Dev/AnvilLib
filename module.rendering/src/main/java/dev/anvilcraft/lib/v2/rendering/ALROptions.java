package dev.anvilcraft.lib.v2.rendering;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("SameParameterValue")
@Slf4j
public class ALROptions {
    public static final boolean SPD_OPTION_WAVE_INTEROP_LDS = getPropertyBoolean("alrSpdOptionUseWaveInteropLds");
    public static final boolean OCCLUSION_QUERY_USE_FRUSTUM_PRE_PASS = getPropertyBoolean("alrOcclusionQueryUseFrustumPrePass", false);
    public static final String OCCLUSION_CULLING_FORCE_IMPL = getProperty("alrOcclusionCullingForceImplementation", null);
    public static final boolean TEXTURE_DEBUG_CLEAR = getPropertyBoolean("alrTextureDebugClear", false);
    public static final boolean DEBUG_CONTEXT = getPropertyBoolean("alrEnableDebugContext", false);
    // public static final int USE_INTEL_BINDLESS_IMAGE_ARRAY_WORKAROUND = getPropertyInt("alrUseIntelBindlessImageArrayWorkaround", 0);

    public static void logAllOptions() {
        log.info("ALR options: SPD_OPTION_WAVE_INTEROP_LDS={}, OCCLUSION_QUERY_USE_FRUSTUM_PRE_PASS={}, OCCLUSION_CULLING_FORCE_IMPL={}, TEXTURE_DEBUG_CLEAR={}",
            // + ", USE_INTEL_BINDLESS_IMAGE_ARRAY_WORKAROUND={}",
            SPD_OPTION_WAVE_INTEROP_LDS,
            OCCLUSION_QUERY_USE_FRUSTUM_PRE_PASS,
            OCCLUSION_CULLING_FORCE_IMPL,
            TEXTURE_DEBUG_CLEAR
            // , USE_INTEL_BINDLESS_IMAGE_ARRAY_WORKAROUND
        );
    }

    private static String getProperty(String key, @Nullable String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    private static boolean getPropertyBoolean(String key, boolean defaultValue) {
        String prop = System.getProperty(key);
        if (prop == null) {
            return defaultValue;
        }
        return !"false".equals(prop);
    }

    private static int getPropertyInt(String key, int defaultValue) {
        String prop = System.getProperty(key);
        if (prop == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(prop);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static boolean getPropertyBoolean(String key) {
        String prop = System.getProperty(key);
        return !"false".equals(prop);
    }
}
