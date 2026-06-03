package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute;

import org.lwjgl.opengl.GL;

public class ALRComputeCapabilities {
    private static boolean COMPUTE_SUPPORTED;

    public static void init() {
        COMPUTE_SUPPORTED = GL.getCapabilities().GL_ARB_compute_shader;
    }

    public static boolean isComputeSupported() {
        return COMPUTE_SUPPORTED;
    }
}
