package dev.anvilcraft.lib.v2.ui;

/**
 * 类型安全的值插值器。
 *
 * @param <T> 插值的值类型
 */
@FunctionalInterface
public interface Interpolator<T> {
    Interpolator<Float> FLOAT = (from, to, t) -> from + (to - from) * t;
    Interpolator<Integer> INT = (from, to, t) -> Math.round(from + (to - from) * t);
    Interpolator<Integer> COLOR = (from, to, t) -> {
        int a = lerp((from >> 24) & 0xFF, (to >> 24) & 0xFF, t);
        int r = lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    };

    T interpolate(T from, T to, float t);

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
