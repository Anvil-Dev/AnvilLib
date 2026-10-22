package dev.anvilcraft.lib.v2.cube.geometry;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 缓存微小错位接缝的位置，按透视投影并集分段保留剪影。 */
final class OutlineVisibility {
    private static final double OFFSET = 0.1D / 16.0D;
    private static final double EPSILON = 2.0E-7D;
    private static final int MAX_VIEW_OPERATIONS = 200_000;
    private final List<ConvexShape> shapes;
    private final List<List<Interval>> seams;

    private OutlineVisibility(List<ConvexShape> shapes, List<List<Interval>> seams) {
        this.shapes = shapes;
        this.seams = seams;
    }

    static OutlineVisibility build(SelectionGeometry geometry, List<OutlineBuilder.Segment> segments, Runnable step) {
        List<List<Interval>> seams = new ArrayList<>(segments.size());
        boolean any = false;
        for (OutlineBuilder.Segment segment : segments) {
            step.run();
            List<Interval> intervals = new ArrayList<>();
            List<ConvexShape> nearby = geometry.intersecting(new AABB(segment.start(), segment.end()).inflate(OFFSET));
            for (int i = 0; i < nearby.size(); i++) {
                for (int j = i + 1; j < nearby.size(); j++) {
                    ConvexShape first = nearby.get(i), second = nearby.get(j);
                    for (ConvexShape.Face a : first.faces()) {
                        for (ConvexShape.Face b : second.faces()) {
                            step.run();
                            double alignment = a.normal().dot(b.normal());
                            if (Math.abs(alignment) < 1.0D - 1.0E-10D) continue;
                            double otherOffset = Math.copySign(1.0D, alignment) * b.planeOffset();
                            double distance = Math.abs(a.planeOffset() - otherOffset);
                            // 模型坐标来自 float；边界误差不得把恰好 0.1 的偏移算作小于 0.1。
                            if (distance <= EPSILON || distance >= OFFSET - EPSILON) continue;
                            double low = Math.min(a.planeOffset(), otherOffset) - EPSILON;
                            double high = Math.max(a.planeOffset(), otherOffset) + EPSILON;
                            Interval interval = new Interval(0, 1);
                            interval = clip(interval, a.normal().dot(segment.start()) - high,
                                a.normal().dot(segment.end()) - high);
                            interval = clip(interval, low - a.normal().dot(segment.start()),
                                low - a.normal().dot(segment.end()));
                            interval = clipShape(interval, segment, first, a.normal(), distance, step);
                            interval = clipShape(interval, segment, second, a.normal(), distance, step);
                            if (interval.valid()) intervals.add(interval);
                        }
                    }
                }
            }
            List<Interval> merged = merge(intervals);
            seams.add(merged);
            any |= !merged.isEmpty();
        }
        return new OutlineVisibility(any ? geometry.shapes() : List.of(), List.copyOf(seams));
    }

    private static Interval clipShape(Interval interval, OutlineBuilder.Segment segment, ConvexShape shape,
                                      Vec3 normal, double distance, Runnable step) {
        for (ConvexShape.Face face : shape.faces()) {
            step.run();
            if (!interval.valid()) break;
            // 只沿错位面的法向扩展，不把相邻的真实细节一起膨胀掉。
            double allowance = Math.abs(face.normal().dot(normal)) * distance + EPSILON;
            interval = clip(interval, face.signedDistance(segment.start()) - allowance,
                face.signedDistance(segment.end()) - allowance);
        }
        return interval;
    }

    long estimatedBytes() {
        return 64L + this.seams.size() * 32L + this.seams.stream().mapToLong(list -> list.size() * 40L).sum();
    }

    PackedOutline forView(PackedOutline outline, Vec3 camera) {
        if (this.shapes.isEmpty()) return outline;
        Work work = new Work();
        try {
            List<List<Vec3>> cones = new ArrayList<>(this.shapes.size());
            for (ConvexShape shape : this.shapes) cones.add(project(shape, camera, work));
            List<OutlineBuilder.Segment> visible = new ArrayList<>();
            for (int index = 0; index < outline.segmentCount(); index++) {
                work.step();
                Vec3 start = point(outline, index, 0), end = point(outline, index, 3);
                List<Interval> hidden = new ArrayList<>();
                if (!this.seams.get(index).isEmpty()) {
                    Vec3 side = start.subtract(camera).cross(end.subtract(start)).normalize();
                    if (side.lengthSqr() > 0.5D) {
                        List<Interval> left = new ArrayList<>(), right = new ArrayList<>();
                        for (List<Vec3> cone : cones) {
                            Interval coverage = new Interval(0, 1);
                            boolean coversLeft = true, coversRight = true;
                            for (Vec3 plane : cone) {
                                work.step();
                                double a = plane.dot(start.subtract(camera)), b = plane.dot(end.subtract(camera));
                                if (Math.abs(a) <= EPSILON && Math.abs(b) <= EPSILON) {
                                    double direction = plane.dot(side);
                                    if (direction > EPSILON) coversLeft = false;
                                    if (direction < -EPSILON) coversRight = false;
                                } else coverage = clip(coverage, a, b);
                                if (!coverage.valid()) break;
                            }
                            if (coverage.valid()) {
                                if (coversLeft) left.add(coverage);
                                if (coversRight) right.add(coverage);
                            }
                        }
                        // 分别求两侧的投影覆盖，才能正确处理两个 cube 共用投影边界的内角。
                        List<Interval> interior = intersect(merge(left), merge(right), work);
                        hidden = intersect(this.seams.get(index), interior, work);
                    }
                }
                double cursor = 0;
                for (Interval interval : hidden) {
                    append(visible, start, end, cursor, interval.minimum());
                    cursor = interval.maximum();
                }
                append(visible, start, end, cursor, 1);
            }
            return PackedOutline.of(visible);
        } catch (ViewBudgetExceeded exception) {
            return outline;
        }
    }

    private static Vec3 point(PackedOutline outline, int index, int offset) {
        return new Vec3(outline.coordinate(index, offset), outline.coordinate(index, offset + 1),
            outline.coordinate(index, offset + 2));
    }

    private static void append(List<OutlineBuilder.Segment> output, Vec3 start, Vec3 end, double low, double high) {
        if (high - low <= EPSILON) return;
        if (output.size() >= SelectionGeometry.MAX_OUTLINE_SEGMENTS) throw new ViewBudgetExceeded();
        output.add(new OutlineBuilder.Segment(start.lerp(end, low), start.lerp(end, high)));
    }

    private static List<Vec3> project(ConvexShape shape, Vec3 camera, Work work) {
        List<Vec3> planes = new ArrayList<>();
        for (ConvexShape.Edge edge : shape.edges()) {
            work.step();
            Vec3 normal = edge.start().subtract(camera).cross(edge.end().subtract(camera));
            if (normal.lengthSqr() < 1.0E-20D) continue;
            normal = normal.normalize();
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (Vec3 vertex : shape.vertices()) {
                work.step();
                double distance = normal.dot(vertex.subtract(camera));
                min = Math.min(min, distance);
                max = Math.max(max, distance);
            }
            if (max <= EPSILON && min < -EPSILON) planes.add(normal);
            else if (min >= -EPSILON && max > EPSILON) planes.add(normal.scale(-1));
        }
        return planes;
    }

    private static Interval clip(Interval interval, double start, double end) {
        if (!interval.valid()) return interval;
        if (start <= 0 && end <= 0) return interval;
        if (start > 0 && end > 0) return new Interval(1, 0);
        double cut = start / (start - end);
        return start > 0 ? new Interval(Math.max(interval.minimum(), cut), interval.maximum())
            : new Interval(interval.minimum(), Math.min(interval.maximum(), cut));
    }

    private static List<Interval> merge(List<Interval> intervals) {
        if (intervals.isEmpty()) return List.of();
        intervals.sort(Comparator.comparingDouble(Interval::minimum));
        List<Interval> result = new ArrayList<>();
        Interval current = intervals.getFirst();
        for (int i = 1; i < intervals.size(); i++) {
            Interval next = intervals.get(i);
            if (next.minimum() <= current.maximum() + EPSILON) {
                current = new Interval(current.minimum(), Math.max(current.maximum(), next.maximum()));
            } else {
                result.add(current);
                current = next;
            }
        }
        result.add(current);
        return List.copyOf(result);
    }

    private static List<Interval> intersect(List<Interval> first, List<Interval> second, Work work) {
        List<Interval> result = new ArrayList<>();
        int a = 0, b = 0;
        while (a < first.size() && b < second.size()) {
            work.step();
            Interval x = first.get(a), y = second.get(b);
            Interval overlap = new Interval(Math.max(x.minimum(), y.minimum()), Math.min(x.maximum(), y.maximum()));
            if (overlap.valid()) result.add(overlap);
            if (x.maximum() < y.maximum()) a++; else b++;
        }
        return result;
    }

    private record Interval(double minimum, double maximum) {
        boolean valid() { return this.maximum - this.minimum > EPSILON; }
    }

    private static final class Work {
        private int operations;
        void step() {
            if (++this.operations > MAX_VIEW_OPERATIONS) throw new ViewBudgetExceeded();
        }
    }

    private static final class ViewBudgetExceeded extends RuntimeException {
        private ViewBudgetExceeded() { super("Outline view budget exceeded", null, false, false); }
    }
}
