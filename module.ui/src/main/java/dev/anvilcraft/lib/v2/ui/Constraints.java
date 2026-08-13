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
        return Math.clamp(w, this.minWidth(), this.maxWidth());
    }

    public float constrainHeight(float h) {
        return Math.clamp(h, this.minHeight(), this.maxHeight());
    }

    public Constraints withWidth(float width) {
        return new Constraints(width, width, this.minHeight(), this.maxHeight());
    }

    public Constraints withHeight(float height) {
        return new Constraints(this.minWidth(), this.maxWidth(), height, height);
    }

    public Constraints copy(float minW, float maxW, float minH, float maxH) {
        return new Constraints(minW, maxW, minH, maxH);
    }
}
