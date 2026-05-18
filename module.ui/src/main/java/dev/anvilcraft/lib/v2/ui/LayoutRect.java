package dev.anvilcraft.lib.v2.ui;

/**
 * 布局阶段之后的定位矩形。
 */
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public record LayoutRect(float x, float y, float width, float height) {
    public static LayoutRect of(float x, float y, float width, float height) {
        return new LayoutRect(x, y, width, height);
    }

    public float right() {
        return this.x() + this.width();
    }

    public float bottom() {
        return this.y() + this.height();
    }

    public boolean contains(float px, float py) {
        return px >= this.x() && px < this.x() + this.width() && py >= this.y() && py < this.y() + this.height();
    }
}
