package dev.anvilcraft.lib.v2.collision;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jetbrains.annotations.ApiStatus;

import static dev.anvilcraft.lib.v2.collision.AnvilLibCollision.intersectsAABBTriangle;
import static dev.anvilcraft.lib.v2.collision.AnvilLibCollision.overlapOnAxis;
import static dev.anvilcraft.lib.v2.collision.AnvilLibCollision.sweptCollisionAABBTriangle;

@ApiStatus.Internal
public class CollisionTest {

    static void main() {
        int[] counts = {
            0,
            0
        }; // [passed, failed]

        // ============================================================
        // PART A -- intersectsAABBTriangle(min, max, triangles, epsilon)
        // AABB: (0,0,0) -> (2,2,2), 2x2x2 cube
        // ============================================================
        Vector3dc boxMin = new Vector3d(0, 0, 0);
        Vector3dc boxMax = new Vector3d(2, 2, 2);

        // A1: triangle fully inside -> true
        {
            Vector3fc[] tris = {
                vf(0.5f, 0.5f, 0.5f),
                vf(1.5f, 0.5f, 0.5f),
                vf(0.5f, 1.5f, 0.5f)
            };
            check(counts, "A1: fully inside", true, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A2: outside +X -> false
        {
            Vector3fc[] tris = {
                vf(3, 0, 0),
                vf(4, 1, 0),
                vf(3, 1, 1)
            };
            check(counts, "A2: outside +X", false, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A3: outside -X -> false
        {
            Vector3fc[] tris = {
                vf(-3, 0, 0),
                vf(-1, 1, 0),
                vf(-2, 1, 1)
            };
            check(counts, "A3: outside -X", false, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A4: outside +Y -> false
        {
            Vector3fc[] tris = {
                vf(0, 3, 0),
                vf(2, 4, 0),
                vf(1, 3, 2)
            };
            check(counts, "A4: outside +Y", false, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A5: outside -Z -> false
        {
            Vector3fc[] tris = {
                vf(0, 0, -2),
                vf(2, 1, -1),
                vf(1, 2, -3)
            };
            check(counts, "A5: outside -Z", false, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A6: piercing through -> true
        {
            Vector3fc[] tris = {
                vf(-1, 1, 1),
                vf(3, 1, 1),
                vf(1, -1, 3)
            };
            check(counts, "A6: piercing through", true, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A7: touching right face (x=2), eps=0 -> true
        {
            Vector3fc[] tris = {
                vf(2, 0, 0),
                vf(2, 2, 0),
                vf(2, 1, 2)
            };
            check(counts, "A7: touching face, eps=0", true, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A8: gap=0.0001, eps=0 -> false; eps=0.001 -> true
        {
            Vector3fc[] tris = {
                vf(2.0001f, 0, 0),
                vf(2.0001f, 2, 0),
                vf(2.0001f, 1, 2)
            };
            check(counts, "A8a: gap 0.0001 eps=0", false, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
            check(counts, "A8b: gap 0.0001 eps=0.001", true, intersectsAABBTriangle(boxMin, boxMax, tris, 0.001));
        }

        // A9: degenerate point inside -> true
        {
            Vector3fc[] tris = {
                vf(1, 1, 1),
                vf(1, 1, 1),
                vf(1, 1, 1)
            };
            check(counts, "A9: point inside", true, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A10: degenerate point outside -> false
        {
            Vector3fc[] tris = {
                vf(5, 5, 5),
                vf(5, 5, 5),
                vf(5, 5, 5)
            };
            check(counts, "A10: point outside", false, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A11: degenerate line intersecting -> true
        {
            Vector3fc[] tris = {
                vf(-1, 1, 1),
                vf(3, 1, 1),
                vf(1, 1, 1)
            };
            check(counts, "A11: line intersecting", true, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A12: degenerate line outside -> false
        {
            Vector3fc[] tris = {
                vf(5, 1, 1),
                vf(7, 1, 1),
                vf(6, 1, 1)
            };
            check(counts, "A12: line outside", false, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A13: large triangle containing AABB (coplanar z=1) -> true
        {
            Vector3fc[] tris = {
                vf(-5, -5, 1),
                vf(7, -5, 1),
                vf(1, 7, 1)
            };
            check(counts, "A13: triangle contains AABB", true, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A14: batch -- only middle triangle intersects -> true
        {
            Vector3fc[] tris = {
                vf(5, 0, 0),
                vf(6, 1, 0),
                vf(5, 1, 1),
                vf(0.5f, 0.5f, 0.5f),
                vf(1.5f, 0.5f, 0.5f),
                vf(0.5f, 1.5f, 1.5f),
                vf(-3, 0, 0),
                vf(-2, 1, 0),
                vf(-3, 1, 1),
                };
            check(counts, "A14: batch mid intersects", true, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A15: batch -- none intersect -> false
        {
            Vector3fc[] tris = {
                vf(5, 0, 0),
                vf(6, 1, 0),
                vf(5, 1, 1),
                vf(-3, 0, 0),
                vf(-2, 1, 0),
                vf(-3, 1, 1),
                };
            check(counts, "A15: batch none intersect", false, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A16: tilted triangle, cross-axis intersection -> true
        {
            Vector3fc[] tris = {
                vf(-0.5f, 2.5f, -0.5f),
                vf(2.5f, 2.5f, -0.5f),
                vf(-0.5f, -1f, 2.5f)
            };
            check(counts, "A16: cross-axis intersection", true, intersectsAABBTriangle(boxMin, boxMax, tris, 0.0));
        }

        // A17: negative-extent AABB -> false
        {
            Vector3dc badMin = new Vector3d(3, 3, 3);
            Vector3dc badMax = new Vector3d(1, 1, 1);
            Vector3fc[] tris = {
                vf(2, 2, 2),
                vf(2, 2, 2),
                vf(2, 2, 2)
            };
            check(counts, "A17: negative-extent AABB", false, intersectsAABBTriangle(badMin, badMax, tris, 0.0));
        }

        // ============================================================
        // PART B -- overlapOnAxis direct unit tests
        // ============================================================
        {
            Vector3d axisX = new Vector3d(1, 0, 0);
            Vector3d halfExt = new Vector3d(1, 1, 1);

            // B1: tri=[0.5,1.5] overlaps b=[-1,1] -> true
            check(
                counts, "B1: overlapOnAxis overlap", true,
                overlapOnAxis(axisX, new Vector3d(0.5, 0, 0), new Vector3d(1.5, 0, 0), new Vector3d(1.0, 1, 0), halfExt, 0.0)
            );

            // B2: tri=[3,5] vs b=[-1,1], separated -> false
            check(
                counts, "B2: overlapOnAxis separated", false,
                overlapOnAxis(axisX, new Vector3d(3, 0, 0), new Vector3d(5, 0, 0), new Vector3d(4, 1, 0), halfExt, 0.0)
            );

            // B3: tri=[1.001,2] vs b=[-1,1], gap=0.001 eps=0 -> false
            check(
                counts, "B3: overlapOnAxis gap eps=0", false,
                overlapOnAxis(axisX, new Vector3d(1.001, 0, 0), new Vector3d(2.0, 0, 0), new Vector3d(1.5, 1, 0), halfExt, 0.0)
            );

            // B4: same gap with eps=0.01 -> true
            check(
                counts, "B4: overlapOnAxis gap eps=0.01", true,
                overlapOnAxis(axisX, new Vector3d(1.001, 0, 0), new Vector3d(2.0, 0, 0), new Vector3d(1.5, 1, 0), halfExt, 0.01)
            );
        }

        // ============================================================
        // PART C -- swept: intersectsAABBTriangle(min,max,motion,tris,eps)
        // AABB: (0,0,0)->(2,2,2), center=(1,1,1), half=(1,1,1)
        // ============================================================

        // C1: no obstacle -> full motion
        {
            Vector3dc motion = new Vector3d(5, 0, 0);
            Vector3fc[] tris = {
                vf(10, 0, 0),
                vf(12, 2, 0),
                vf(10, 2, 2)
            };
            Vector3dc result = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "C1: no obstacle +X", new Vector3d(5, 0, 0), result);
        }

        // C2: wall at x=5 blocks +X motion, AABB.maxX reaches 5 at t=3
        {
            Vector3dc motion = new Vector3d(5, 0, 0);
            Vector3fc[] tris = {
                vf(5, 0, 0),
                vf(5, 2, 0),
                vf(5, 1, 2)
            };
            Vector3dc result = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "C2: blocked +X at x=5", new Vector3d(3, 0, 0), result);
        }

        // C3: wall at x=-2 blocks -X motion, AABB.minX reaches -2 at t=-2
        {
            Vector3dc motion = new Vector3d(-5, 0, 0);
            Vector3fc[] tris = {
                vf(-2, 0, 0),
                vf(-2, 2, 0),
                vf(-2, 1, 2)
            };
            Vector3dc result = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "C3: blocked -X at x=-2", new Vector3d(-2, 0, 0), result);
        }

        // C4: initial overlap -> 0 for that axis
        {
            Vector3dc motion = new Vector3d(5, 0, 0);
            Vector3fc[] tris = {
                vf(0.5f, 0.5f, 0.5f),
                vf(1.5f, 0.5f, 0.5f),
                vf(0.5f, 1.5f, 0.5f)
            };
            Vector3dc result = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "C4: initial overlap -> 0", new Vector3d(0, 0, 0), result);
        }

        // C5: wall at y=5 blocks +Y, AABB.maxY=2, reaches at t=3
        {
            Vector3dc motion = new Vector3d(0, 5, 0);
            Vector3fc[] tris = {
                vf(0, 5, 0),
                vf(2, 5, 0),
                vf(1, 5, 2)
            };
            Vector3dc result = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "C5: blocked +Y at y=5", new Vector3d(0, 3, 0), result);
        }

        // C6: wall at z=5 blocks +Z
        {
            Vector3dc motion = new Vector3d(0, 0, 5);
            Vector3fc[] tris = {
                vf(0, 0, 5),
                vf(2, 0, 5),
                vf(1, 2, 5)
            };
            Vector3dc result = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "C6: blocked +Z at z=5", new Vector3d(0, 0, 3), result);
        }

        // C7: diag motion, X blocked, Y free
        {
            Vector3dc motion = new Vector3d(5, 5, 0);
            Vector3fc[] tris = {
                vf(5, 0, 0),
                vf(5, 2, 0),
                vf(5, 1, 10)
            };
            Vector3dc result = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "C7: +X blocked, +Y free", new Vector3d(3, 5, 0), result);
        }

        // C8: epsilon bridges small gap -> full motion returned
        {
            Vector3dc motion = new Vector3d(5, 0, 0);
            Vector3fc[] tris = {
                vf(2.5f, 0, 0),
                vf(2.5f, 2, 0),
                vf(2.5f, 1, 2)
            };
            Vector3dc r0 = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            Vector3dc r1 = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 1.0);
            check3(counts, "C8a: gap=0.5 eps=0 blocked at 0.5", new Vector3d(0.5, 0, 0), r0);
            check3(counts, "C8b: gap=0.5 eps=1 initial overlap", new Vector3d(0, 0, 0), r1);
        }

        // C9: moving away from obstacle -> full motion
        {
            Vector3dc motion = new Vector3d(-5, 0, 0);
            Vector3fc[] tris = {
                vf(5, 0, 0),
                vf(5, 2, 0),
                vf(5, 1, 2)
            };
            Vector3dc result = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "C9: moving away full motion", new Vector3d(-5, 0, 0), result);
        }

        // C10: X wall at x=5 + Y wall at y=5 — each blocks its axis
        {
            Vector3dc motion = new Vector3d(5, 5, 0);
            Vector3fc[] tris = {
                vf(5, 0, 0), vf(5, 2, 0), vf(5, 1, 10),   // wall at x=5, blocks X
                vf(0, 5, 0), vf(2, 5, 0), vf(1, 5, 10),   // wall at y=5, blocks Y
            };
            Vector3dc result = intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "C10: diag blocked X and Y", new Vector3d(3, 3, 0), result);
        }

        // ============================================================
        // PART D -- true swept: sweptCollisionAABBTriangle(min,max,motion,tris,eps)
        // AABB: (0,0,0)->(2,2,2), center=(1,1,1)
        // Key difference from per-axis: sweeps along the motion direction
        // vector, not independently per axis
        // ============================================================

        // D1: axis-aligned motion, wall at x=5 → same result as per-axis C2
        {
            Vector3dc motion = new Vector3d(5, 0, 0);
            Vector3fc[] tris = { vf(5, 0, 0), vf(5, 2, 0), vf(5, 1, 2) };
            Vector3dc result = sweptCollisionAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "D1: axis-aligned blocked +X", new Vector3d(3, 0, 0), result);
        }

        // D2: diagonal slides past narrow wall → not blocked (per-axis would give (3,5,0))
        {
            Vector3dc motion = new Vector3d(5, 5, 0);
            Vector3fc[] tris = { vf(5, 0, 0), vf(5, 2, 0), vf(5, 1, 2) };
            Vector3dc result = sweptCollisionAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "D2: diag slides past narrow wall", new Vector3d(5, 5, 0), result);
        }

        // D3: initial overlap → zero
        {
            Vector3dc motion = new Vector3d(5, 0, 0);
            Vector3fc[] tris = { vf(0.5f, 0.5f, 0.5f), vf(1.5f, 0.5f, 0.5f), vf(0.5f, 1.5f, 0.5f) };
            Vector3dc result = sweptCollisionAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "D3: initial overlap", new Vector3d(0, 0, 0), result);
        }

        // D4: no obstacle → full diagonal motion
        {
            Vector3dc motion = new Vector3d(5, 5, 0);
            Vector3fc[] tris = { vf(20, 0, 0), vf(22, 2, 0), vf(20, 2, 2) };
            Vector3dc result = sweptCollisionAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "D4: no obstacle diag", new Vector3d(5, 5, 0), result);
        }

        // D5: diagonal blocked by large wall → blocked at t=0.6 → motion * 0.6
        {
            Vector3dc motion = new Vector3d(5, 5, 0);
            Vector3fc[] tris = { vf(5, 0, 0), vf(5, 10, 0), vf(5, 5, 10) };
            Vector3dc result = sweptCollisionAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "D5: diag blocked by large wall", new Vector3d(3, 3, 0), result);
        }

        // D6: zero-length motion → zero vector
        {
            Vector3dc motion = new Vector3d(0, 0, 0);
            Vector3fc[] tris = { vf(5, 0, 0), vf(5, 2, 0), vf(5, 1, 2) };
            Vector3dc result = sweptCollisionAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            check3(counts, "D6: zero motion", new Vector3d(0, 0, 0), result);
        }

        // D7: epsilon bridges gap → initial overlap → zero
        {
            Vector3dc motion = new Vector3d(5, 0, 0);
            Vector3fc[] tris = { vf(2.5f, 0, 0), vf(2.5f, 2, 0), vf(2.5f, 1, 2) };
            Vector3dc r = sweptCollisionAABBTriangle(boxMin, boxMax, motion, tris, 1.0);
            check3(counts, "D7: gap=0.5 eps=1 initial overlap", new Vector3d(0, 0, 0), r);
        }

        // ============================================================
        // Report
        // ============================================================
        System.out.println("========================================");
        System.out.println("Passed: " + counts[0] + " / " + (counts[0] + counts[1]));
        if (counts[1] > 0) {
            System.out.println("FAILED: " + counts[1]);
        } else {
            System.out.println("ALL TESTS PASSED");
        }
        System.out.println("========================================");

        bench();
    }

    // ================================================================
    // Performance benchmark
    // ================================================================
    private static void bench() {
        Vector3dc boxMin = new Vector3d(0, 0, 0);
        Vector3dc boxMax = new Vector3d(2, 2, 2);
        Vector3dc motion = new Vector3d(0.5, 0.3, 0.2);
        int[] triCounts = {1, 10, 100, 1000, 10000};
        int warmupIters = 2000;

        System.out.println();
        System.out.println("==================== BENCHMARK ====================");
        System.out.printf("%-8s | %-16s | %-16s | %-16s%n",
            "tris", "static SAT", "per-axis swept", "true swept");
        System.out.println("---------+------------------+------------------+------------------");

        for (int n : triCounts) {
            Vector3fc[] tris = generateTriangles(n, 42 + n);

            // Determine iteration count per scenario
            int iters = Math.max(200, 100000 / Math.max(1, n));

            // Warmup
            for (int i = 0; i < warmupIters; i++) {
                intersectsAABBTriangle(boxMin, boxMax, tris, 0.0);
                intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
                sweptCollisionAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            }

            // Static SAT
            long t0 = System.nanoTime();
            for (int i = 0; i < iters; i++) {
                intersectsAABBTriangle(boxMin, boxMax, tris, 0.0);
            }
            long t1 = System.nanoTime();
            double staticNs = (double) (t1 - t0) / iters;

            // Per-axis swept
            long t2 = System.nanoTime();
            for (int i = 0; i < iters; i++) {
                intersectsAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            }
            long t3 = System.nanoTime();
            double perAxisNs = (double) (t3 - t2) / iters;

            // True swept
            long t4 = System.nanoTime();
            for (int i = 0; i < iters; i++) {
                sweptCollisionAABBTriangle(boxMin, boxMax, motion, tris, 0.0);
            }
            long t5 = System.nanoTime();
            double trueSweptNs = (double) (t5 - t4) / iters;

            System.out.printf("%-8d | %8.1f ns/op | %8.1f ns/op | %8.1f ns/op%n",
                n, staticNs, perAxisNs, trueSweptNs);
        }
        System.out.println("====================================================");
        System.out.println("AABB: (0,0,0)->(2,2,2)  motion: (0.5,0.3,0.2)  eps: 0");
        System.out.println("Triangles: random mix in [-10,12] cube, ~50% intersect");
        System.out.println();
    }

    /**
     * Generates {@code count} triangles in a pseudo-random but deterministic
     * pattern.  Roughly half will intersect the AABB (0,0,0)→(2,2,2) so both
     * the early-out and full-SAT paths are exercised.
     */
    private static Vector3fc[] generateTriangles(int count, long seed) {
        Vector3fc[] tris = new Vector3fc[count * 3];
        long state = seed;
        for (int i = 0; i < count; i++) {
            // LCG: state = state * 6364136223846793005L + 1442695040888963407L
            state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;

            // Pick a center in [-10, 12] for each axis
            double cx = -10.0 + ((state >>> 16) & 0x7FFFFF) / 524287.0 * 22.0;
            state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;
            double cy = -10.0 + ((state >>> 16) & 0x7FFFFF) / 524287.0 * 22.0;
            state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;
            double cz = -10.0 + ((state >>> 16) & 0x7FFFFF) / 524287.0 * 22.0;

            // Random offsets for 3 vertices within a 3-unit radius
            double[][] offsets = new double[3][3];
            for (int v = 0; v < 3; v++) {
                for (int a = 0; a < 3; a++) {
                    state = state * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;
                    offsets[v][a] = -1.5 + ((state >>> 16) & 0x7FFFFF) / 524287.0 * 3.0;
                }
            }

            tris[i * 3]     = new Vector3f((float)(cx + offsets[0][0]), (float)(cy + offsets[0][1]), (float)(cz + offsets[0][2]));
            tris[i * 3 + 1] = new Vector3f((float)(cx + offsets[1][0]), (float)(cy + offsets[1][1]), (float)(cz + offsets[1][2]));
            tris[i * 3 + 2] = new Vector3f((float)(cx + offsets[2][0]), (float)(cy + offsets[2][1]), (float)(cz + offsets[2][2]));
        }
        return tris;
    }

    private static void check(int[] c, String name, boolean expected, boolean actual) {
        if (expected == actual) {
            System.out.println("[PASS] " + name);
            c[0]++;
        } else {
            System.out.println("[FAIL] " + name + " -- expected " + expected + ", got " + actual);
            c[1]++;
        }
    }

    private static void check3(int[] c, String name, Vector3dc expected, Vector3dc actual) {
        double tol = 1e-9;
        boolean ok = Math.abs(expected.x() - actual.x()) < tol
                     && Math.abs(expected.y() - actual.y()) < tol
                     && Math.abs(expected.z() - actual.z()) < tol;
        if (ok) {
            System.out.println("[PASS] " + name);
            c[0]++;
        } else {
            System.out.printf(
                "[FAIL] %s -- expected (%f,%f,%f), got (%f,%f,%f)%n",
                name, expected.x(), expected.y(), expected.z(),
                actual.x(), actual.y(), actual.z()
            );
            c[1]++;
        }
    }

    private static Vector3f vf(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }
}
