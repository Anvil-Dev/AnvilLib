package dev.anvilcraft.lib.v2.util;

public abstract class MathUtil {
    public static float safeDiv(float a, float b) {
        if (b == 0F) return 0F;
        return a / b;
    }

    public static double safeDiv(double a, double b) {
        if (b == 0.0) return 0.0;
        return a / b;
    }
}
