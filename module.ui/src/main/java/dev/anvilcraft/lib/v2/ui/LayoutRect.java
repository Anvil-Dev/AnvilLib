package dev.anvilcraft.lib.v2.ui;

/**
 * A positioned rectangle after the layout pass.
 */
public record LayoutRect(float x, float y, float width, float height) {

    public static LayoutRect of(float x, float y, float width, float height) {
        return new LayoutRect(x, y, width, height);
    }

    public float right() { return x + width; }

    public float bottom() { return y + height; }

    public boolean contains(float px, float py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
}
