package dev.anvilcraft.lib.v2.util;

import dev.anvilcraft.lib.v2.util.client.Line;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class OutlineUtil {
    private static final String PARALLEL_THRESHOLD_PROPERTY = "anvillib.outline.parallelThreshold";
    private static final int DEFAULT_PARALLEL_BOX_THRESHOLD = 1024;

    private OutlineUtil() {
    }

    public static List<Line> extractOutline(List<AABB> boxes) {
        return extractOutline(boxes, null);
    }

    public static List<Line> extractOutline(List<AABB> boxes, @Nullable Executor executor) {
        if (boxes.isEmpty()) {
            return List.of();
        }

        EdgeAccumulator accumulator = new EdgeAccumulator();
        List<AxisTask> tasks = createAxisTasks(boxes);
        if (boxes.size() < parallelBoxThreshold()) {
            for (AxisTask task : tasks) {
                accumulator.addAll(task.extract());
            }
        } else {
            for (EdgeAccumulator extracted : extractParallel(tasks, executor)) {
                accumulator.addAll(extracted);
            }
        }
        return accumulator.toLines();
    }

    private static List<EdgeAccumulator> extractParallel(List<AxisTask> tasks, @Nullable Executor executor) {
        if (executor == null) {
            return tasks.parallelStream().map(AxisTask::extract).toList();
        }
        return tasks.stream()
            .map(task -> CompletableFuture.supplyAsync(task::extract, executor))
            .map(CompletableFuture::join)
            .toList();
    }

    private static int parallelBoxThreshold() {
        return Integer.getInteger(PARALLEL_THRESHOLD_PROPERTY, DEFAULT_PARALLEL_BOX_THRESHOLD);
    }

    private static List<AxisTask> createAxisTasks(List<AABB> boxes) {
        List<AxisTask> tasks = new ArrayList<>(3);
        for (int axis = 0; axis < 3; axis++) {
            tasks.add(new AxisTask(boxes, axis, firstOtherAxis(axis), secondOtherAxis(axis)));
        }
        return tasks;
    }

    private record AxisTask(List<AABB> boxes, int axis, int uAxis, int vAxis) {
        private EdgeAccumulator extract() {
            TreeSet<Double> planes = new TreeSet<>();
            Map<Double, List<Rect>> starts = new HashMap<>();
            Map<Double, List<Rect>> ends = new HashMap<>();
            for (AABB box : boxes) {
                if (isDegenerate(box)) {
                    continue;
                }
                double min = min(box, axis);
                double max = max(box, axis);
                Rect projection = project(box, uAxis, vAxis);
                planes.add(min);
                planes.add(max);
                starts.computeIfAbsent(min, ignored -> new ArrayList<>()).add(projection);
                ends.computeIfAbsent(max, ignored -> new ArrayList<>()).add(projection);
            }

            EdgeAccumulator accumulator = new EdgeAccumulator();
            RectMultiset active = new RectMultiset();
            for (double plane : planes) {
                List<Rect> lower = active.toList();
                active.removeAll(ends.getOrDefault(plane, List.of()));
                active.addAll(starts.getOrDefault(plane, List.of()));
                extractPlaneOutline(axis, uAxis, vAxis, plane, lower, active.toList(), accumulator);
            }
            return accumulator;
        }
    }

    private static void extractPlaneOutline(
        int axis,
        int uAxis,
        int vAxis,
        double plane,
        List<Rect> lower,
        List<Rect> upper,
        EdgeAccumulator accumulator
    ) {
        if (lower.isEmpty() && upper.isEmpty()) {
            return;
        }

        AxisGrid grid = AxisGrid.create(lower, upper);
        if (grid.uCoordinates.length < 2 || grid.vCoordinates.length < 2) {
            return;
        }

        BitSet[] lowerRows = createRows(grid.uSize());
        BitSet[] upperRows = createRows(grid.uSize());
        mark(lowerRows, lower, grid);
        mark(upperRows, upper, grid);

        for (int u = 0; u < grid.uSize(); u++) {
            for (int v = 0; v < grid.vSize(); v++) {
                if (empty(lowerRows, upperRows, u, v)) {
                    continue;
                }

                if (v == 0 || empty(lowerRows, upperRows, u, v - 1)) {
                    accumulator.add(
                        point(axis, uAxis, vAxis, plane, grid.uCoordinates[u], grid.vCoordinates[v]),
                        point(axis, uAxis, vAxis, plane, grid.uCoordinates[u + 1], grid.vCoordinates[v])
                    );
                }
                if (v == grid.vSize() - 1 || empty(lowerRows, upperRows, u, v + 1)) {
                    accumulator.add(
                        point(axis, uAxis, vAxis, plane, grid.uCoordinates[u], grid.vCoordinates[v + 1]),
                        point(axis, uAxis, vAxis, plane, grid.uCoordinates[u + 1], grid.vCoordinates[v + 1])
                    );
                }
                if (u == 0 || empty(lowerRows, upperRows, u - 1, v)) {
                    accumulator.add(
                        point(axis, uAxis, vAxis, plane, grid.uCoordinates[u], grid.vCoordinates[v]),
                        point(axis, uAxis, vAxis, plane, grid.uCoordinates[u], grid.vCoordinates[v + 1])
                    );
                }
                if (u == grid.uSize() - 1 || empty(lowerRows, upperRows, u + 1, v)) {
                    accumulator.add(
                        point(axis, uAxis, vAxis, plane, grid.uCoordinates[u + 1], grid.vCoordinates[v]),
                        point(axis, uAxis, vAxis, plane, grid.uCoordinates[u + 1], grid.vCoordinates[v + 1])
                    );
                }
            }
        }
    }

    private static BitSet[] createRows(int size) {
        BitSet[] rows = new BitSet[size];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new BitSet();
        }
        return rows;
    }

    private static void mark(BitSet[] rows, List<Rect> rects, AxisGrid grid) {
        for (Rect rect : rects) {
            Integer minU = grid.uIndex.get(rect.minU);
            Integer maxU = grid.uIndex.get(rect.maxU);
            Integer minV = grid.vIndex.get(rect.minV);
            Integer maxV = grid.vIndex.get(rect.maxV);
            if (minU == null || maxU == null || minV == null || maxV == null) {
                continue;
            }
            for (int u = minU; u < maxU; u++) {
                rows[u].set(minV, maxV);
            }
        }
    }

    private static boolean empty(BitSet[] lowerRows, BitSet[] upperRows, int u, int v) {
        return lowerRows[u].get(v) == upperRows[u].get(v);
    }

    private static Rect project(AABB box, int uAxis, int vAxis) {
        return new Rect(
            canonical(min(box, uAxis)),
            canonical(max(box, uAxis)),
            canonical(min(box, vAxis)),
            canonical(max(box, vAxis))
        );
    }

    private static Point point(int axis, int uAxis, int vAxis, double plane, double u, double v) {
        double[] coordinates = new double[3];
        coordinates[axis] = plane;
        coordinates[uAxis] = u;
        coordinates[vAxis] = v;
        return new Point(coordinates[0], coordinates[1], coordinates[2]);
    }

    private static boolean isDegenerate(AABB box) {
        return box.minX >= box.maxX || box.minY >= box.maxY || box.minZ >= box.maxZ;
    }

    private static int firstOtherAxis(int axis) {
        return switch (axis) {
            case 0 -> 1;
            case 1, 2 -> 0;
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }

    private static int secondOtherAxis(int axis) {
        return switch (axis) {
            case 0, 1 -> 2;
            case 2 -> 1;
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }

    private static double min(AABB box, int axis) {
        return switch (axis) {
            case 0 -> box.minX;
            case 1 -> box.minY;
            case 2 -> box.minZ;
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }

    private static double max(AABB box, int axis) {
        return switch (axis) {
            case 0 -> box.maxX;
            case 1 -> box.maxY;
            case 2 -> box.maxZ;
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }

    private static double canonical(double value) {
        return value == 0.0 ? 0.0 : value;
    }

    private record Rect(double minU, double maxU, double minV, double maxV) {
    }

    private static final class RectMultiset {
        private final Map<Rect, Integer> counts;

        private RectMultiset() {
            this.counts = new HashMap<>();
        }

        private void addAll(List<Rect> rects) {
            for (Rect rect : rects) {
                counts.merge(rect, 1, Integer::sum);
            }
        }

        private void removeAll(List<Rect> rects) {
            for (Rect rect : rects) {
                counts.computeIfPresent(rect, (ignored, count) -> count == 1 ? null : count - 1);
            }
        }

        private List<Rect> toList() {
            return new ArrayList<>(counts.keySet());
        }
    }

    private record Point(double x, double y, double z) {
        private Vec3 toVec3() {
            return new Vec3(x, y, z);
        }
    }

    private record AxisGrid(
        double[] uCoordinates,
        double[] vCoordinates,
        Map<Double, Integer> uIndex,
        Map<Double, Integer> vIndex
    ) {
        private static AxisGrid create(List<Rect> lower, List<Rect> upper) {
            TreeSet<Double> uCoordinates = new TreeSet<>();
            TreeSet<Double> vCoordinates = new TreeSet<>();
            addCoordinates(lower, uCoordinates, vCoordinates);
            addCoordinates(upper, uCoordinates, vCoordinates);
            double[] uArray = toArray(uCoordinates);
            double[] vArray = toArray(vCoordinates);
            return new AxisGrid(uArray, vArray, index(uArray), index(vArray));
        }

        private int uSize() {
            return uCoordinates.length - 1;
        }

        private int vSize() {
            return vCoordinates.length - 1;
        }

        private static void addCoordinates(List<Rect> rects, Set<Double> uCoordinates, Set<Double> vCoordinates) {
            for (Rect rect : rects) {
                uCoordinates.add(rect.minU);
                uCoordinates.add(rect.maxU);
                vCoordinates.add(rect.minV);
                vCoordinates.add(rect.maxV);
            }
        }

        private static double[] toArray(TreeSet<Double> coordinates) {
            double[] result = new double[coordinates.size()];
            int index = 0;
            for (double coordinate : coordinates) {
                result[index++] = coordinate;
            }
            return result;
        }

        private static Map<Double, Integer> index(double[] coordinates) {
            Map<Double, Integer> result = new HashMap<>();
            for (int i = 0; i < coordinates.length; i++) {
                result.put(coordinates[i], i);
            }
            return result;
        }
    }

    private static final class EdgeAccumulator {
        private final Set<Segment> segments = new HashSet<>();

        private void add(Point first, Point second) {
            if (first.equals(second)) {
                return;
            }
            segments.add(Segment.create(first, second));
        }

        private void addAll(EdgeAccumulator other) {
            segments.addAll(other.segments);
        }

        private List<Line> toLines() {
            Map<LineKey, List<Interval>> intervals = new HashMap<>();
            for (Segment segment : segments) {
                intervals.computeIfAbsent(segment.key(), ignored -> new ArrayList<>()).add(segment.interval());
            }

            List<Line> result = new ArrayList<>();
            for (Map.Entry<LineKey, List<Interval>> entry : intervals.entrySet()) {
                LineKey key = entry.getKey();
                List<Interval> lineIntervals = entry.getValue();
                lineIntervals.sort(Comparator.comparingDouble(Interval::min).thenComparingDouble(Interval::max));

                double min = lineIntervals.getFirst().min;
                double max = lineIntervals.getFirst().max;
                for (int i = 1; i < lineIntervals.size(); i++) {
                    Interval interval = lineIntervals.get(i);
                    if (interval.min <= max) {
                        max = Math.max(max, interval.max);
                    } else {
                        result.add(key.toLine(min, max));
                        min = interval.min;
                        max = interval.max;
                    }
                }
                result.add(key.toLine(min, max));
            }
            return result;
        }
    }

    private record Segment(Point from, Point to) {
        private static Segment create(Point first, Point second) {
            int axis = varyingAxis(first, second);
            if (coordinate(first, axis) <= coordinate(second, axis)) {
                return new Segment(first, second);
            }
            return new Segment(second, first);
        }

        private LineKey key() {
            int axis = varyingAxis(from, to);
            return new LineKey(axis, coordinate(from, fixedAxis(axis, 0)), coordinate(from, fixedAxis(axis, 1)));
        }

        private Interval interval() {
            int axis = varyingAxis(from, to);
            return new Interval(coordinate(from, axis), coordinate(to, axis));
        }

        private static int varyingAxis(Point from, Point to) {
            if (from.x != to.x) return 0;
            if (from.y != to.y) return 1;
            if (from.z != to.z) return 2;
            throw new IllegalArgumentException("Zero-length segment");
        }
    }

    private record LineKey(int axis, double firstFixed, double secondFixed) {
        private Line toLine(double min, double max) {
            double[] from = new double[3];
            double[] to = new double[3];
            from[axis] = min;
            to[axis] = max;
            from[fixedAxis(axis, 0)] = firstFixed;
            to[fixedAxis(axis, 0)] = firstFixed;
            from[fixedAxis(axis, 1)] = secondFixed;
            to[fixedAxis(axis, 1)] = secondFixed;
            return new Line(new Point(from[0], from[1], from[2]).toVec3(), new Point(to[0], to[1], to[2]).toVec3());
        }
    }

    private record Interval(double min, double max) {
    }

    private static int fixedAxis(int varyingAxis, int fixedIndex) {
        return switch (varyingAxis) {
            case 0 -> fixedIndex == 0 ? 1 : 2;
            case 1 -> fixedIndex == 0 ? 0 : 2;
            case 2 -> fixedIndex == 0 ? 0 : 1;
            default -> throw new IllegalArgumentException("Unknown axis: " + varyingAxis);
        };
    }

    private static double coordinate(Point point, int axis) {
        return switch (axis) {
            case 0 -> point.x;
            case 1 -> point.y;
            case 2 -> point.z;
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }
}
