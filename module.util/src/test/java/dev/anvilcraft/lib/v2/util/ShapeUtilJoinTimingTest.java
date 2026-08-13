package dev.anvilcraft.lib.v2.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import dev.anvilcraft.lib.v2.util.client.Line;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public final class ShapeUtilJoinTimingTest {
    private static final int AABB_COUNT = 500;
    private static final long RANDOM_SEED = 0x5A5A5A5AL;

    private ShapeUtilJoinTimingTest() {
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        assertOutlineMatchesShapePath(List.of(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)), "single box");
        assertOutlineMatchesShapePath(
            List.of(
                new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
                new AABB(0.5, 0.5, 0.0, 1.5, 1.5, 1.0)
            ),
            "overlapping boxes"
        );
        assertOutlineMatchesShapePath(
            List.of(
                new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
                new AABB(1.0, 0.0, 0.0, 2.0, 1.0, 1.0)
            ),
            "face-adjacent boxes"
        );
        assertOutlineMatchesShapePath(
            List.of(
                new AABB(0.0, 0.0, 0.0, 2.0, 2.0, 2.0),
                new AABB(0.5, 0.5, 0.5, 1.5, 1.5, 1.5)
            ),
            "contained box"
        );
        assertOutlineMatchesShapePath(
            List.of(
                new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
                new AABB(2.0, 0.0, 0.0, 3.0, 1.0, 1.0)
            ),
            "disconnected boxes"
        );
        assertOutlineMatchesShapePath(createConnectedDiagonalAabbs(24, RANDOM_SEED), "connected diagonal chain");
        assertParallelMatchesSequential(createConnectedDiagonalAabbs(64, RANDOM_SEED));
        assertCustomExecutorParallelMatchesSequential(createConnectedDiagonalAabbs(64, RANDOM_SEED));

        List<AABB> boxes = createConnectedDiagonalAabbs(AABB_COUNT, RANDOM_SEED);
        JoinTimingResult shapeResult = joinAndExtractOutlineByShape(boxes);
        OutlineTimingResult aabbResult = extractOutlineByAabbs(boxes);

        assertEquals(AABB_COUNT, boxes.size(), "generated AABB count");
        assertFalse(shapeResult.joined().isEmpty(), "joined shape should not be empty");
        assertNotNull(shapeResult.outline(), "shape outline result");
        assertNotNull(aabbResult.outline(), "AABB outline result");
        assertSameLines(shapeResult.outline(), aabbResult.outline(), "benchmark outline");
    }

    private static void assertOutlineMatchesShapePath(List<AABB> boxes, String label)
        throws ExecutionException, InterruptedException {
        JoinTimingResult shapeResult = joinAndExtractOutlineByShape(boxes);
        OutlineTimingResult aabbResult = extractOutlineByAabbs(boxes);
        assertSameLines(shapeResult.outline(), aabbResult.outline(), label);
    }

    private static void assertParallelMatchesSequential(List<AABB> boxes) {
        String propertyName = "anvillib.outline.parallelThreshold";
        String previous = System.getProperty(propertyName);
        try {
            System.setProperty(propertyName, Integer.toString(Integer.MAX_VALUE));
            List<Line> sequential = OutlineUtil.extractOutline(boxes);
            System.setProperty(propertyName, "1");
            List<Line> parallel = OutlineUtil.extractOutline(boxes);
            assertEquals(sequential, parallel, "parallel outline branch");
        } finally {
            if (previous == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, previous);
            }
        }
    }

    private static void assertCustomExecutorParallelMatchesSequential(List<AABB> boxes) {
        String propertyName = "anvillib.outline.parallelThreshold";
        String previous = System.getProperty(propertyName);
        int[] executions = {0};
        Executor countingExecutor = command -> {
            executions[0]++;
            command.run();
        };
        try {
            System.setProperty(propertyName, Integer.toString(Integer.MAX_VALUE));
            List<Line> sequential = OutlineUtil.extractOutline(boxes);
            System.setProperty(propertyName, "1");
            List<Line> parallel = OutlineUtil.extractOutline(boxes, countingExecutor);
            if (executions[0] <= 0) {
                throw new AssertionError("custom executor should run parallel outline tasks");
            }
            assertEquals(sequential, parallel, "custom executor parallel outline branch");
        } finally {
            if (previous == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, previous);
            }
        }
    }

    private static JoinTimingResult joinAndExtractOutlineByShape(List<AABB> boxes) throws ExecutionException, InterruptedException {
        List<VoxelShape> shapes = boxes.stream()
            .map(box -> Shapes.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ))
            .toList();
        long started = System.nanoTime();
        VoxelShape result;
        try (var executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1))) {
            result = ShapeUtil.threadedJoin(shapes, BooleanOp.OR, executor).get();
        }
        List<Line> outline = new ArrayList<>();
        result.forAllEdges(
            (x1, y1, z1, x2, y2, z2) -> outline.add(
                new Line(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2))
            )
        );
        long elapsedNanos = System.nanoTime() - started;
        System.out.printf(
            "Shape path: thread-joined %d connected AABBs into %d lines in %.3f ms.%n",
            boxes.size(),
            outline.size(),
            elapsedNanos / 1_000_000.0
        );
        return new JoinTimingResult(result, outline);
    }

    private static OutlineTimingResult extractOutlineByAabbs(List<AABB> boxes) {
        long started = System.nanoTime();
        List<Line> outline = OutlineUtil.extractOutline(boxes);
        long elapsedNanos = System.nanoTime() - started;
        System.out.printf(
            "AABB path: extracted %d connected AABBs into %d lines in %.3f ms.%n",
            boxes.size(),
            outline.size(),
            elapsedNanos / 1_000_000.0
        );
        return new OutlineTimingResult(outline);
    }

    private static List<AABB> createConnectedDiagonalAabbs(int count, long seed) {
        Random random = new Random(seed);
        List<AABB> boxes = new ArrayList<>(count);
        double x = 0.0;
        double y = 0.0;
        double size = 1.0;
        boxes.add(new AABB(x, y, 0.0, x + size, y + size, 1.0));

        for (int i = 1; i < count; i++) {
            double nextSize = 0.7 + random.nextDouble() * 0.6;
            double step = Math.min(size, nextSize) * (0.45 + random.nextDouble() * 0.1);
            x += step;
            y += step;
            boxes.add(new AABB(x, y, 0.0, x + nextSize, y + nextSize, 1.0));
            size = nextSize;
        }
        return boxes;
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(List<Line> expected, List<Line> actual, String label) {
        if (!expected.equals(actual)) {
            List<Line> missing = new ArrayList<>(expected);
            missing.removeAll(actual);
            List<Line> extra = new ArrayList<>(actual);
            extra.removeAll(expected);
            throw new AssertionError(
                label + ": expected " + expected.size() + " lines, got " + actual.size()
                    + ", missing=" + missing
                    + ", extra=" + extra
            );
        }
    }

    private static void assertSameLines(List<Line> expected, List<Line> actual, String label) {
        Set<Line> expectedSet = new HashSet<>(expected);
        Set<Line> actualSet = new HashSet<>(actual);
        if (!expectedSet.equals(actualSet)) {
            Set<Line> missing = new HashSet<>(expectedSet);
            missing.removeAll(actualSet);
            Set<Line> extra = new HashSet<>(actualSet);
            extra.removeAll(expectedSet);
            throw new AssertionError(
                label + ": expected " + expectedSet.size() + " lines, got " + actualSet.size()
                    + ", missing=" + missing
                    + ", extra=" + extra
            );
        }
    }

    private static void assertFalse(boolean value, String label) {
        if (value) {
            throw new AssertionError(label);
        }
    }

    private static void assertNotNull(Object value, String label) {
        if (value == null) {
            throw new AssertionError(label + " should not be null");
        }
    }

    private record JoinTimingResult(VoxelShape joined, List<Line> outline) {
    }

    private record OutlineTimingResult(List<Line> outline) {
    }
}
