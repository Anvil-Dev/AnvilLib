package dev.anvilcraft.lib.v2.cube.geometry;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4dc;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 与客户端模型、实体及物理求解器无关的闭凸多面体，坐标以方块为单位。 */
public final class ConvexShape {
    private static final int[][] BOX_FACES = {
        {0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1},
        {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 6, 7, 5}
    };
    private final List<Vec3> vertices;
    private final int[][] indices;
    private final List<Face> faces;
    private final List<Edge> edges;
    private final AABB bounds;
    private final int hash;

    public ConvexShape(List<Vec3> vertices, int[][] faces) {
        if (vertices.size() < 4 || vertices.size() > 256 || faces.length < 4 || faces.length > 256) {
            throw new IllegalArgumentException("Expected a bounded, closed convex polyhedron");
        }
        this.vertices = List.copyOf(vertices);
        this.indices = Arrays.stream(faces).map(int[]::clone).toArray(int[][]::new);
        Vec3 center = Vec3.ZERO;
        AABB bounds = null;
        for (Vec3 vertex : this.vertices) {
            if (!Double.isFinite(vertex.x) || !Double.isFinite(vertex.y) || !Double.isFinite(vertex.z)) {
                throw new IllegalArgumentException("Vertices must be finite");
            }
            center = center.add(vertex.scale(1.0 / vertices.size()));
            AABB point = new AABB(vertex, vertex);
            bounds = bounds == null ? point : bounds.minmax(point);
        }
        this.bounds = Objects.requireNonNull(bounds);
        List<Face> planes = new ArrayList<>(faces.length);
        List<Edge> edges = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int[] face : this.indices) {
            if (face.length < 3 || face.length > 256) throw new IllegalArgumentException("Invalid face");
            for (int index : face) {
                if (index < 0 || index >= vertices.size()) throw new IllegalArgumentException("Invalid vertex index");
            }
            Vec3 first = vertices.get(face[0]);
            Vec3 normal = Vec3.ZERO;
            for (int i = 1; i + 1 < face.length && normal.lengthSqr() < 1.0E-20; i++) {
                normal = vertices.get(face[i]).subtract(first).cross(vertices.get(face[i + 1]).subtract(first));
            }
            if (normal.lengthSqr() < 1.0E-20) throw new IllegalArgumentException("Degenerate face");
            normal = normal.normalize();
            if (normal.dot(center.subtract(first)) > 0) normal = normal.scale(-1);
            Face plane = new Face(normal, normal.dot(first));
            if (plane.signedDistance(center) >= -1.0E-10) throw new IllegalArgumentException("Zero-volume shape");
            for (Vec3 vertex : vertices) {
                if (plane.signedDistance(vertex) > 1.0E-6) throw new IllegalArgumentException("Non-convex shape");
            }
            planes.add(plane);
            for (int i = 0; i < face.length; i++) {
                int a = face[i], b = face[(i + 1) % face.length];
                long key = ((long) Math.min(a, b) << 32) | Math.max(a, b);
                if (seen.add(key)) edges.add(new Edge(vertices.get(a), vertices.get(b)));
            }
        }
        this.faces = List.copyOf(planes);
        this.edges = List.copyOf(edges);
        this.hash = 31 * this.vertices.hashCode() + Arrays.deepHashCode(this.indices);
    }

    public static ConvexShape box(AABB bounds) {
        List<Vec3> vertices = new ArrayList<>(8);
        for (int corner = 0; corner < 8; corner++) {
            vertices.add(new Vec3(
                (corner & 1) == 0 ? bounds.minX : bounds.maxX,
                (corner & 2) == 0 ? bounds.minY : bounds.maxY,
                (corner & 4) == 0 ? bounds.minZ : bounds.maxZ
            ));
        }
        return new ConvexShape(vertices, BOX_FACES);
    }

    public ConvexShape transform(Matrix4dc transform) {
        List<Vec3> moved = new ArrayList<>(this.vertices.size());
        Vector3d value = new Vector3d();
        for (Vec3 vertex : this.vertices) {
            transform.transformPosition(value.set(vertex.x, vertex.y, vertex.z));
            moved.add(new Vec3(value.x, value.y, value.z));
        }
        return new ConvexShape(moved, this.indices);
    }

    public List<Vec3> vertices() { return this.vertices; }
    public List<Face> faces() { return this.faces; }
    public List<Edge> edges() { return this.edges; }
    public AABB bounds() { return this.bounds; }

    public boolean contains(Vec3 point, double epsilon) {
        for (Face face : this.faces) if (face.signedDistance(point) > epsilon) return false;
        return true;
    }

    /** 保守估算包含对象、数组和拓扑的占用，供缓存准入使用。 */
    public long estimatedBytes() {
        return 256L + this.vertices.size() * 56L + this.faces.size() * 112L + this.edges.size() * 40L
            + Arrays.stream(this.indices).mapToLong(face -> 24L + face.length * 4L).sum();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ConvexShape shape && this.hash == shape.hash
            && this.vertices.equals(shape.vertices) && Arrays.deepEquals(this.indices, shape.indices);
    }

    @Override
    public int hashCode() { return this.hash; }

    public record Face(Vec3 normal, double planeOffset) {
        public double signedDistance(Vec3 point) { return normal.dot(point) - planeOffset; }
    }

    public record Edge(Vec3 start, Vec3 end) { }
}
