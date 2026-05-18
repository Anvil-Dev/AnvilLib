package dev.anvilcraft.lib.v2.ui;

/**
 * Result of a component's measure pass.
 */
public record MeasuredSize(float width, float height) {

    public static final MeasuredSize ZERO = new MeasuredSize(0, 0);

    public static MeasuredSize of(float width, float height) {
        return new MeasuredSize(width, height);
    }
}
