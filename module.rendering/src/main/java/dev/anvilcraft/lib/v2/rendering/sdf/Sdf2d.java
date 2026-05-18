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
        var rect        = params.getRect();
        var round       = params.getRound();
        var smooth      = params.getSmooth();
        var stroke      = params.getStroke();

        var ex          = (round + smooth + stroke) * 2.0f;

        var width       = rect.z + ex;
        var height      = rect.w + ex;

        float cx;
        float cy;

        if (params.isCenter()) {

            cx          = rect.x;
            cy          = rect.y;

        } else {

            cx          = rect.x + width * 0.5f;
            cy          = rect.y + height * 0.5f;
        }

        var px          = x - cx;
        var py          = y - cy;

        var rotation    = params.getRotation();
        if (rotation    != 0f) {

            var r       = -rotation * Mth.DEG_TO_RAD;

            var s       = (float)Math.sin(r);
            var c       = (float)Math.cos(r);

            var tx      = px * c - py * s;
            var ty      = px * s + py * c;

            px          = tx;
            py          = ty;
        }


        var type        = params.getRenderType();
        var shape       = params.getShapeParams();

        var d           = switch (type) {
            case BOX -> sdRect(
                    px, py,
                    shape.x - round,
                    shape.y - round
            ) - round;

            case CIRCLE -> sdCircle(
                    px, py,
                    shape.x
            );

            case ARC -> sdArc(
                    px, py,
                    shape.x, shape.y,
                    shape.z, shape.w
            ) - round;

            case SECTOR -> sdRing(
                    px, py,
                    shape.x, shape.y,
                    shape.z, shape.w
            ) - round;

            case PIE -> sdPie(
                    px, py,
                    shape.x, shape.y,
                    shape.z
            ) - round;

            case CAPSULE -> sdUnevenCapsule(
                    px, py,
                    shape.x, shape.y,
                    shape.z
            );

            case EGG -> sdEgg(
                    px, py,
                    shape.x, shape.y,
                    shape.z
            );

            case TRIANGLE -> sdTriangle(
                    px, py,
                    shape.x, shape.y,
                    shape.z, shape.w,
                    Float.intBitsToFloat(params.getTypeParams().w),
                    params.getSharedParams().w
            );
        };

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

        float dx        = Math.abs(px) - bx;
        float dy        = Math.abs(py) - by;

        float mx        = Math.max(dx, 0.0f);
        float my        = Math.max(dy, 0.0f);

        return          Mth.length(mx, my) +
                        Math.min(Math.max(dx, dy), 0.0f);
    }

    public static float sdCircle(
            float px, float py,
            float r
    ) {
        return          Mth.length(px, py) - r;
    }

    public static float sdArc(
            float px, float py,
            float scx, float scy,
            float ra, float rb
    ) {

        px              = Math.abs(px);

        float result;

        if (scy * px > scx * py) {
            var dx      = px - scx * ra;
            var dy      = py - scy * ra;
            result      = Mth.length(dx, dy);
        } else {
            result      = Math.abs(Mth.length(px, py) - ra);
        }

        return          result - rb;
    }

    public static float sdRing(
            float px, float py,
            float nx, float ny,
            float r, float th
    ) {

        px              = Math.abs(px);

        float rx        = nx * px + (-ny) * py;
        float ry        = ny * px +  nx  * py;

        float a         = Math.abs(Mth.length(rx, ry) - r) - th * 0.5f;

        float by        = Math.max(0.0f,
                        Math.abs(r - ry) - th * 0.5f);

        float b         = Mth.length(rx, by) *
                        Mth.sign(rx);

        return          Math.max(a, b);
    }

    public static float sdPie(
            float px, float py,
            float cx, float cy,
            float r
    ) {

        px =            Math.abs(px);

        float l =       Mth.length(px, py) - r;

        float dot       = px * cx + py * cy;
        float clamped   = Mth.clamp(dot, 0.0f, r);

        float mx        = px - cx * clamped;
        float my        = py - cy * clamped;

        float m         = Mth.length(mx, my);

        return          Math.max(
                            l,
                            m * Mth.sign(cy * px - cx * py)
                        );
    }

    public static float sdUnevenCapsule(
            float px, float py,
            float r1,
            float r2,
            float h
    ) {

        px              = Math.abs(px);

        float b         = (r1 - r2) / h;
        float a         = (float)Math.sqrt(1.0f - b * b);
        float k         = px * (-b) + py * a;

        if (k           < 0.0f) {
            return      Mth.length(px, py) - r1;
        }

        if (k           > a * h) {

            float dx    = px;
            float dy    = py - h;

            return      Mth.length(dx, dy) - r2;
        }

        return          px * a + py * b - r1;
    }

    public static float sdEgg(
            float px, float py,
            float he,
            float ra,
            float rb
    ) {

        float ce        =
                        0.5f * (
                                he * he
                                        - (ra - rb) * (ra - rb)
                        ) / (ra - rb);

        px              = Math.abs(px);

        if (py          < 0.0f) {

            return      Mth.length(px, py) - ra;
        }

        if (py * ce - px * he > he * ce) {

            return      Mth.length(
                            px,
                            py - he
                        ) - rb;
        }

        return          Mth.length(
                            px + ce,
                            py
                        ) - (ce + ra);
    }

    /**
     * 三角形 SDF。来自 <a href="https://iquilezles.org/articles/distfunctions2d/">IQ</a>。
     */
    public static float sdTriangle(
            float px, float py,
            float x0, float y0,
            float x1, float y1,
            float x2, float y2
    ) {
        float ex0 = x1 - x0, ey0 = y1 - y0;
        float ex1 = x2 - x1, ey1 = y2 - y1;
        float ex2 = x0 - x2, ey2 = y0 - y2;
        float e0x = px - x0, e0y = py - y0;
        float e1x = px - x1, e1y = py - y1;
        float e2x = px - x2, e2y = py - y2;

        float v0 = e0x * ey0 - e0y * ex0;
        float v1 = e1x * ey1 - e1y * ex1;
        float v2 = e2x * ey2 - e2y * ex2;

        float d;
        if (v0 * v1 > 0.0f && v1 * v2 > 0.0f) {
            d = -Math.min(Math.min(
                    (e0x * ex0 + e0y * ey0) / (float) Math.sqrt(ex0 * ex0 + ey0 * ey0),
                    (e1x * ex1 + e1y * ey1) / (float) Math.sqrt(ex1 * ex1 + ey1 * ey1)),
                    (e2x * ex2 + e2y * ey2) / (float) Math.sqrt(ex2 * ex2 + ey2 * ey2));
        } else {
            d = (float) Math.sqrt(Math.min(Math.min(
                    e0x * e0x + e0y * e0y,
                    e1x * e1x + e1y * e1y),
                    e2x * e2x + e2y * e2y));
        }
        return d;
    }

}
