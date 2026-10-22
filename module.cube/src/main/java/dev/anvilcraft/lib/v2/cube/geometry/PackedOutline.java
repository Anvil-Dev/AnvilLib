package dev.anvilcraft.lib.v2.cube.geometry;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/** 每条棱线保存两个端点和单位方向；缓存不持有世界坐标。 */
public final class PackedOutline {
    public static final PackedOutline EMPTY = new PackedOutline(new float[0]);
    public static final int STRIDE = 9;
    private final float[] vertices;
    private final @Nullable OutlineVisibility visibility;

    private PackedOutline(float[] vertices) { this(vertices, null); }

    private PackedOutline(float[] vertices, @Nullable OutlineVisibility visibility) {
        this.vertices = vertices;
        this.visibility = visibility;
    }

    static PackedOutline of(List<OutlineBuilder.Segment> segments, SelectionGeometry geometry, Runnable step) {
        PackedOutline outline = of(segments);
        return new PackedOutline(outline.vertices, OutlineVisibility.build(geometry, segments, step));
    }

    /** 相机必须与轮廓处于同一局部坐标系；不会修改缓存中的原始棱线。 */
    public PackedOutline forView(Vec3 camera) {
        return this.visibility == null ? this : this.visibility.forView(this, camera);
    }

    public static PackedOutline of(List<OutlineBuilder.Segment> segments) {
        float[] vertices = new float[Math.multiplyExact(segments.size(), STRIDE)];
        int offset = 0;
        for (OutlineBuilder.Segment segment : segments) {
            Vec3 delta = segment.end().subtract(segment.start());
            double length = delta.length();
            if (length <= 1.0E-9) continue;
            vertices[offset++] = (float) segment.start().x;
            vertices[offset++] = (float) segment.start().y;
            vertices[offset++] = (float) segment.start().z;
            vertices[offset++] = (float) segment.end().x;
            vertices[offset++] = (float) segment.end().y;
            vertices[offset++] = (float) segment.end().z;
            vertices[offset++] = (float) (delta.x / length);
            vertices[offset++] = (float) (delta.y / length);
            vertices[offset++] = (float) (delta.z / length);
        }
        return offset == 0 ? EMPTY : new PackedOutline(offset == vertices.length ? vertices : Arrays.copyOf(vertices, offset));
    }

    public int segmentCount() { return this.vertices.length / STRIDE; }
    public long estimatedBytes() { return 48L + this.vertices.length * 4L + (this.visibility == null ? 0 : this.visibility.estimatedBytes()); }
    public float coordinate(int segment, int component) { return this.vertices[segment * STRIDE + component]; }
}
