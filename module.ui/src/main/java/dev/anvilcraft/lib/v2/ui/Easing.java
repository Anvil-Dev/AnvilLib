package dev.anvilcraft.lib.v2.ui;

/**
 * 缓动函数。接受归一化时间 t ∈ [0,1]，返回缓动后的进度。
 */
@FunctionalInterface
public interface Easing {
    Easing LINEAR = t -> t;
    Easing EASE_IN = t -> t * t;
    Easing EASE_OUT = t -> t * (2 - t);
    Easing EASE_IN_OUT = t -> t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
    Easing BOUNCE = t -> {
        if (t < 1f / 2.75f) return 7.5625f * t * t;
        if (t < 2f / 2.75f) { t -= 1.5f / 2.75f; return 7.5625f * t * t + 0.75f; }
        if (t < 2.5f / 2.75f) { t -= 2.25f / 2.75f; return 7.5625f * t * t + 0.9375f; }
        t -= 2.625f / 2.75f;
        return 7.5625f * t * t + 0.984375f;
    };

    float apply(float t);
}
