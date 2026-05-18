package dev.anvilcraft.lib.v2.ui;

/**
 * 父容器传给子组件的 min/max 尺寸约束。
 */
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public record Constraints(float minWidth, float maxWidth, float minHeight, float maxHeight) {
    public static final Constraints NONE = new Constraints(0, Float.MAX_VALUE, 0, Float.MAX_VALUE);

    public float constrainWidth(float w) {
        return Math.clamp(w, minWidth, maxWidth);
    }

    public float constrainHeight(float h) {
        return Math.clamp(h, minHeight, maxHeight);
    }

    public Constraints withWidth(float width) {
        return new Constraints(width, width, minHeight, maxHeight);
    }

    public Constraints withHeight(float height) {
        return new Constraints(minWidth, maxWidth, height, height);
    }

    public Constraints copy(float minW, float maxW, float minH, float maxH) {
        return new Constraints(minW, maxW, minH, maxH);
    }
}
