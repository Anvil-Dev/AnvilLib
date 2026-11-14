package dev.anvilcraft.lib.v2.rendering.preview.gui;

import org.joml.Vector2f;

/** 浮窗连线与可中断的短时过渡所用的纯几何运算。 */
public final class StructureGuiGeometry {
    private StructureGuiGeometry() {
    }

    /** 从窗口中心朝锚点发出的射线与窗口边缘的交点；锚点在窗口内时不绘制连线。 */
    public static Vector2f edge(float x, float y, float width, float height, float anchorX, float anchorY) {
        float cx = x + width / 2, cy = y + height / 2;
        float dx = anchorX - cx, dy = anchorY - cy;
        if (Math.abs(dx) <= width / 2 && Math.abs(dy) <= height / 2) {
            return new Vector2f(anchorX, anchorY);
        }
        float factor = Math.min(dx == 0 ? Float.POSITIVE_INFINITY : width / 2 / Math.abs(dx),
            dy == 0 ? Float.POSITIVE_INFINITY : height / 2 / Math.abs(dy));
        return new Vector2f(cx + dx * factor, cy + dy * factor);
    }

    /** cubic-bezier(0.23, 1, 0.32, 1)，按横坐标求值。 */
    public static float easeOut(float progress) {
        float x = Math.clamp(progress, 0, 1);
        float low = 0, high = 1;
        for (int i = 0; i < 16; i++) {
            float t = (low + high) / 2;
            float inv = 1 - t;
            float curveX = 3 * inv * inv * t * 0.23f + 3 * inv * t * t * 0.32f + t * t * t;
            if (curveX < x) {
                low = t;
            }
            else {
                high = t;
            }
        }
        float t = (low + high) / 2;
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }
}
