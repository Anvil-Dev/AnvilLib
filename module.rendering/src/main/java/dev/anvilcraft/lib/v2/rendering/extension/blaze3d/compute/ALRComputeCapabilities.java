package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRHICapabilities;

public class ALRComputeCapabilities {
    private static boolean COMPUTE_SUPPORTED;

    public static void init() {
        COMPUTE_SUPPORTED = ALRHICapabilities.getInstance().compute();
    }

    public static boolean isComputeSupported() {
        return COMPUTE_SUPPORTED;
    }
}
