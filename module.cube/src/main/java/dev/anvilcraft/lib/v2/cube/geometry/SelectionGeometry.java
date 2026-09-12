package dev.anvilcraft.lib.v2.cube.geometry;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 不可变局部几何；BVH 同时服务于射线窄相位及后台棱线生成。 */
public final class SelectionGeometry {
    public static final int MAX_SHAPES = 1024;
    public static final int MAX_OUTLINE_SEGMENTS = 4096;
    private final List<ConvexShape> shapes;
    private final Node root;
    private final PackedOutline fallback;
    private final long estimatedBytes;

    public SelectionGeometry(List<ConvexShape> shapes) {
        if (shapes.isEmpty() || shapes.size() > MAX_SHAPES) throw new IllegalArgumentException("Invalid shape count");
        this.shapes = List.copyOf(shapes);
        this.root = build(new ArrayList<>(shapes));
        List<OutlineBuilder.Segment> edges = new ArrayList<>();
        int count = shapes.stream().mapToInt(shape -> shape.edges().size()).sum();
        List<ConvexShape> preview = count <= MAX_OUTLINE_SEGMENTS ? shapes : List.of(ConvexShape.box(this.bounds()));
        for (ConvexShape shape : preview) {
            for (ConvexShape.Edge edge : shape.edges()) edges.add(new OutlineBuilder.Segment(edge.start(), edge.end()));
        }
        this.fallback = PackedOutline.of(edges);
        this.estimatedBytes = 160L + shapes.size() * 256L + this.fallback.estimatedBytes()
            + shapes.stream().mapToLong(ConvexShape::estimatedBytes).sum();
    }

    public List<ConvexShape> shapes() { return this.shapes; }
    public AABB bounds() { return this.root.bounds; }
    public PackedOutline fallbackOutline() { return this.fallback; }
    public long estimatedBytes() { return this.estimatedBytes; }

    public List<ConvexShape> intersecting(AABB bounds) {
        List<ConvexShape> result = new ArrayList<>();
        query(this.root, bounds, result);
        return result;
    }

    public @Nullable RayHit clip(Vec3 start, Vec3 end) {
        return clip(this.root, start, end.subtract(start), null);
    }

    private static @Nullable RayHit clip(Node node, Vec3 start, Vec3 delta, @Nullable RayHit best) {
        double maximum = best == null ? 1.0 : best.fraction;
        if (!intersectsRay(node.bounds, start, delta, maximum)) return best;
        if (node.shape == null) {
            RayHit first = clip(node.left, start, delta, best);
            return clip(node.right, start, delta, first);
        }
        double enter = 0, exit = maximum;
        Vec3 normal = null;
        boolean inside = true;
        for (ConvexShape.Face face : node.shape.faces()) {
            double distance = face.signedDistance(start);
            if (distance > 1.0E-9) inside = false;
            double speed = face.normal().dot(delta);
            if (Math.abs(speed) < 1.0E-12) {
                if (distance > 1.0E-9) return best;
                continue;
            }
            double fraction = -distance / speed;
            if (speed < 0) {
                if (fraction >= enter) { enter = fraction; normal = face.normal(); }
            } else exit = Math.min(exit, fraction);
            if (enter > exit + 1.0E-10) return best;
        }
        if (enter < 0 || enter > maximum || exit < 0) return best;
        return new RayHit(enter, normal == null ? delta.scale(-1).normalize() : normal, inside);
    }

    private static boolean intersectsRay(AABB box, Vec3 start, Vec3 delta, double maximum) {
        double low = 0, high = maximum;
        for (int axis = 0; axis < 3; axis++) {
            double origin = axis == 0 ? start.x : axis == 1 ? start.y : start.z;
            double speed = axis == 0 ? delta.x : axis == 1 ? delta.y : delta.z;
            double min = axis == 0 ? box.minX : axis == 1 ? box.minY : box.minZ;
            double max = axis == 0 ? box.maxX : axis == 1 ? box.maxY : box.maxZ;
            if (Math.abs(speed) < 1.0E-12) {
                if (origin < min - 1.0E-9 || origin > max + 1.0E-9) return false;
            } else {
                double a = (min - origin) / speed, b = (max - origin) / speed;
                low = Math.max(low, Math.min(a, b));
                high = Math.min(high, Math.max(a, b));
                if (low > high + 1.0E-10) return false;
            }
        }
        return true;
    }

    private static Node build(List<ConvexShape> shapes) {
        AABB bounds = shapes.getFirst().bounds();
        for (int i = 1; i < shapes.size(); i++) bounds = bounds.minmax(shapes.get(i).bounds());
        if (shapes.size() == 1) return new Node(bounds, shapes.getFirst(), null, null);
        int axis = bounds.getXsize() >= bounds.getYsize() && bounds.getXsize() >= bounds.getZsize() ? 0
            : bounds.getYsize() >= bounds.getZsize() ? 1 : 2;
        shapes.sort(Comparator.comparingDouble(shape -> {
            AABB box = shape.bounds();
            return axis == 0 ? box.minX + box.maxX : axis == 1 ? box.minY + box.maxY : box.minZ + box.maxZ;
        }));
        int split = shapes.size() / 2;
        return new Node(bounds, null, build(shapes.subList(0, split)), build(shapes.subList(split, shapes.size())));
    }

    private static void query(Node node, AABB bounds, List<ConvexShape> output) {
        if (!node.bounds.intersects(bounds)) return;
        if (node.shape != null) output.add(node.shape);
        else { query(node.left, bounds, output); query(node.right, bounds, output); }
    }

    public record RayHit(double fraction, Vec3 normal, boolean inside) { }
    private record Node(AABB bounds, @Nullable ConvexShape shape, @Nullable Node left, @Nullable Node right) { }
}
