package dev.anvilcraft.lib.v2.rendering.sdf;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Sdf2d {

    public static float sd(
            @NotNull SdfParameters params,
            float x, float y
    ) {
        var rect    = params.getRect();
        var round   = params.getRound();
        var smooth  = params.getSmooth();
        var stroke  = params.getStroke();

        var center  = params.isCenter();
        var rotate  = params.getRotation();

        var rx      = rect.x;
        var ry      = rect.y;
        var rw      = rect.z;
        var rh      = rect.w;

        float cx;
        float cy;

        if (center) {
            cx = rx;
            cy = ry;
        } else {
            // 左上角 -> 中心
            cx = rx + rw * 0.5f;
            cy = ry + rh * 0.5f;
        }

        // ----------------------------
        // 2. world -> local
        // ----------------------------

        float px = x - cx;
        float py = y - cy;

        // ----------------------------
        // 3. 逆旋转
        // ----------------------------

        if (rotate != 0f) {

            float s = (float)Math.sin(-rotate);
            float c = (float)Math.cos(-rotate);

            float tx = px * c - py * s;
            float ty = px * s + py * c;

            px = tx;
            py = ty;
        }

        // ----------------------------
        // 4. 如果不是center
        //    修正局部坐标
        // ----------------------------

        // SDF 默认认为：
        // 图形中心在 (0,0)

        // 但非center模式下：
        // 图形实际是从左上角开始绘制

        // 因此需要偏移回去

        if (!center) {
            px -= rw * 0.5f;
            py -= rh * 0.5f;
        }


        var type    = params.getRenderType();

        var shape   = params.getShapeParams();

        var d       = 1e5f;
        switch (type) {
            case BOX:
                d = sdRect(
                        x, y,
                        shape.x - round,
                        shape.y - round
                ) - round;
                break;

            case CIRCLE:
                d = sdCircle(
                        px, py,
                        shape.x
                );
                break;

            case ARC:
                d = sdArc(
                        px, py,
                        shape.x, shape.y,
                        shape.z, shape.w
                ) - round;
                break;

            case SECTOR:
                d = sdRing(
                        px, py,
                        shape.x, shape.y,
                        shape.z, shape.w
                ) - round;
                break;

            case PIE:
                d = sdPie(
                        px, py,
                        shape.x, shape.y,
                        shape.z
                ) - round;
                break;

        }

        if (params.isOnion()) {
            var half    = stroke * 0.5f;
            d           = Mth.abs(d) - half;
        }

        return d;
    }

    public static float sdRect(
            float px, float py,
            float bx, float by
    ) {

        float dx = Math.abs(px) - bx;
        float dy = Math.abs(py) - by;

        float mx = Math.max(dx, 0.0f);
        float my = Math.max(dy, 0.0f);

        return Mth.length(mx, my)
                + Math.min(Math.max(dx, dy), 0.0f);
    }

    public static float sdCircle(
            float px, float py,
            float r
    ) {
        return Mth.length(px, py) - r;
    }

    public static float sdArc(
            float px, float py,
            float scx, float scy,
            float ra, float rb
    ) {

        px = Math.abs(px);

        float result;

        if (scy * px > scx * py) {
            float dx = px - scx * ra;
            float dy = py - scy * ra;
            result = Mth.length(dx, dy);
        } else {
            result = Math.abs(Mth.length(px, py) - ra);
        }

        return result - rb;
    }

    public static float sdRing(
            float px, float py,
            float nx, float ny,
            float r, float th
    ) {

        px = Math.abs(px);

        // mat2(n.x,n.y,-n.y,n.x) * p
        float rx = nx * px + ny * py;
        float ry = -ny * px + nx * py;

        float a = Math.abs(Mth.length(rx, ry) - r) - th * 0.5f;

        float bx = rx;
        float by = Math.max(0.0f,
                Math.abs(r - ry) - th * 0.5f);

        float b = Mth.length(bx, by) * Mth.sign(rx);

        return Math.max(a, b);
    }

    public static float sdPie(
            float px, float py,
            float cx, float cy,
            float r
    ) {

        px = Math.abs(px);

        float l = Mth.length(px, py) - r;

        float dot = px * cx + py * cy;
        float clamped = Mth.clamp(dot, 0.0f, r);

        float mx = px - cx * clamped;
        float my = py - cy * clamped;

        float m = Mth.length(mx, my);

        return Math.max(
                l,
                m * Mth.sign(cy * px - cx * py)
        );
    }

}
