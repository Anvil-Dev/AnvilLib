package dev.anvilcraft.lib.v2.ui;

/**
 * 组件 measure 阶段的结果。
 */
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public record MeasuredSize(float width, float height) {
    public static final MeasuredSize ZERO = new MeasuredSize(0, 0);

    public static MeasuredSize of(float width, float height) {
        return new MeasuredSize(width, height);
    }
}
