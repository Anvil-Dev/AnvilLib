package dev.anvilcraft.lib.v2.cube.geometry;

import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutlineVisibilityTest {
    private static final double SMALL = 0.05D / 16;

    @Test
    void hidesBothConvexAndConcaveOffsetSeams() {
        PackedOutline outline = build(step(SMALL));
        Vec3 convex = new Vec3(0.5, 1 + SMALL, 0.5), concave = new Vec3(0.5, 1, 0.5);
        assertTrue(covers(outline, convex));
        assertTrue(covers(outline, concave));
        PackedOutline visible = outline.forView(new Vec3(0.75, 8, 0.5));
        assertFalse(covers(visible, convex));
        assertFalse(covers(visible, concave));
        assertTrue(covers(visible, new Vec3(0, 1, 0.5)));
    }

    @Test
    void thresholdIsStrictlyLessThanOneTenthOfAModelUnit() {
        for (double offset : new double[] {0.1D / 16, 0.10001D / 16, 0.2D / 16}) {
            PackedOutline outline = build(step(offset));
            assertTrue(covers(outline.forView(new Vec3(0.75, 8, 0.5)), new Vec3(0.5, 1 + offset, 0.5)));
        }
        double below = 0.09999D / 16;
        assertFalse(covers(build(step(below)).forView(new Vec3(0.75, 8, 0.5)), new Vec3(0.5, 1 + below, 0.5)));
    }

    @Test
    void floatModelCoordinatesAtTheThresholdAreNotSmoothed() {
        double height = (float) 16.1 / 16.0;
        assertTrue(covers(build(step(height - 1)).forView(new Vec3(0.75, 8, 0.5)), new Vec3(0.5, height, 0.5)));
    }

    @Test
    void aSingleCubeAndLargeDetailsKeepTheirEdges() {
        PackedOutline outline = build(List.of(box(0, 0, 0, 1, 1, 1)));
        assertEquals(12, outline.segmentCount());
        assertSame(outline, outline.forView(new Vec3(3, 4, 5)));
        PackedOutline detail = build(step(0.5));
        assertTrue(covers(detail.forView(new Vec3(0.75, 8, 0.5)), new Vec3(0.5, 1.5, 0.5)));
    }

    @Test
    void coplanarSharedSeamsStayAbsent() {
        PackedOutline outline = build(List.of(box(0, 0, 0, 1, 1, 1), box(1, 0, 0, 2, 1, 1)));
        assertEquals(12, outline.segmentCount());
        assertFalse(covers(outline.forView(new Vec3(3, 4, 5)), new Vec3(1, 1, 0.5)));
    }

    @Test
    void sameEdgeChangesBetweenInteriorAndSilhouetteWithCamera() {
        List<ConvexShape> shapes = List.of(box(0, 0, 0, 1, 1, 1), box(0.5, 0, 0.25, 1, 1 + SMALL, 0.75));
        PackedOutline outline = build(shapes);
        Vec3 point = new Vec3(1, 1 + SMALL, 0.5);
        assertTrue(covers(outline, point));
        assertTrue(covers(outline.forView(new Vec3(-3, 3, 0.5)), point));
        assertFalse(covers(outline.forView(new Vec3(3, 3, 0.5)), point));
        assertTrue(covers(outline, point));
    }

    @Test
    void splitsOneSeamAtProjectedSilhouetteBoundaries() {
        List<ConvexShape> shapes = List.of(box(0, 0, 0, 1, 1, 1),
            box(0.5, 0, 0.1, 1, 1 + SMALL, 0.9), box(1.1, 0, 0.4, 2, 1.2, 0.6));
        PackedOutline outline = build(shapes);
        PackedOutline visible = outline.forView(new Vec3(-3, 3, 0.5));
        assertTrue(covers(visible, new Vec3(1, 1 + SMALL, 0.2)));
        assertFalse(covers(visible, new Vec3(1, 1 + SMALL, 0.5)));
        assertTrue(covers(visible, new Vec3(1, 1 + SMALL, 0.8)));
    }

    @Test
    void onlyThePortionNearAnotherCubeIsSmoothed() {
        PackedOutline outline = build(List.of(box(0, 0, 0, 1, 1, 1), box(0.5, 0, -0.5, 1.5, 1 + SMALL, 1.5)));
        PackedOutline visible = outline.forView(new Vec3(0.75, 8, 0.5));
        assertTrue(covers(visible, new Vec3(0.5, 1 + SMALL, -0.25)));
        assertFalse(covers(visible, new Vec3(0.5, 1 + SMALL, 0.5)));
        assertTrue(covers(visible, new Vec3(0.5, 1 + SMALL, 1.25)));
    }

    @Test
    void rotatedCubesUseTheirFacePlanes() {
        Matrix4d rotation = new Matrix4d().rotateXYZ(0.2, 0.4, -0.3);
        PackedOutline outline = build(step(SMALL).stream().map(shape -> shape.transform(rotation)).toList());
        Vec3 point = transform(rotation, new Vec3(0.5, 1 + SMALL, 0.5));
        assertTrue(covers(outline, point));
        assertFalse(covers(outline.forView(transform(rotation, new Vec3(0.75, 8, 0.5))), point));
    }

    @Test
    void nearbyButUnrelatedFacesKeepTheirEdges() {
        PackedOutline outline = build(List.of(box(0, 0, 0, 1, 1, 1), box(2, 0, 0, 3, 1 + SMALL, 1)));
        assertTrue(covers(outline.forView(new Vec3(0.5, 8, 0.5)), new Vec3(0, 1, 0.5)));
    }

    @Test
    void narrowProjectedCoverageIsNotLostBetweenSamples() {
        List<ConvexShape> shapes = List.of(box(0, 0, 0, 1, 1, 1),
            box(0.5, 0, 0.1, 1, 1 + SMALL, 0.9), box(1.1, 0, 0.5001, 2, 1.2, 0.5003));
        PackedOutline visible = build(shapes).forView(new Vec3(-3, 3, 0.5002));
        assertTrue(covers(visible, new Vec3(1, 1 + SMALL, 0.499)));
        assertFalse(covers(visible, new Vec3(1, 1 + SMALL, 0.5002)));
        assertTrue(covers(visible, new Vec3(1, 1 + SMALL, 0.501)));
    }

    @Test
    void nearContactFacesHideInteriorEdgesButKeepTheSilhouette() {
        PackedOutline outline = build(List.of(box(0, 0, 0, 1, 1, 1), box(1 + SMALL, 0, 0, 2, 1, 1)));
        PackedOutline visible = outline.forView(new Vec3(-3, 4, 3));
        assertFalse(covers(visible, new Vec3(1, 1, 0.5)));
        assertTrue(covers(visible, new Vec3(0, 1, 0.5)));
    }

    @Test
    void transformedPartsConvertTheCameraToGeometrySpace() {
        SelectionGeometry geometry = new SelectionGeometry(step(SMALL));
        Matrix4f matrix = new Matrix4f().translation(4, -2, 1).rotateY(0.7F).scale(-2, 3, 0.5F);
        SelectionPart part = new SelectionPart(geometry, matrix);
        Vec3 camera = new Vec3(0.75, 8, 0.5);
        Vec3 local = part.localPosition(transform(new Matrix4d(matrix), camera));
        assertEquals(0, camera.distanceTo(local), 1.0E-5);
        assertFalse(covers(build(step(SMALL)).forView(local), new Vec3(0.5, 1 + SMALL, 0.5)));
    }

    private static List<ConvexShape> step(double offset) {
        return List.of(box(0, 0, 0, 1, 1, 1), box(0.5, 0, 0.25, 1.5, 1 + offset, 0.75));
    }

    private static ConvexShape box(double x, double y, double z, double maxX, double maxY, double maxZ) {
        return ConvexShape.box(new AABB(x, y, z, maxX, maxY, maxZ));
    }

    private static PackedOutline build(List<ConvexShape> shapes) {
        return OutlineBuilder.build(new SelectionGeometry(shapes), TimeUnit.SECONDS.toNanos(10), 10_000_000);
    }

    private static Vec3 transform(Matrix4d matrix, Vec3 point) {
        var result = matrix.transformPosition(new org.joml.Vector3d(point.x, point.y, point.z));
        return new Vec3(result.x, result.y, result.z);
    }

    private static boolean covers(PackedOutline outline, Vec3 point) {
        for (int i = 0; i < outline.segmentCount(); i++) {
            Vec3 start = new Vec3(outline.coordinate(i, 0), outline.coordinate(i, 1), outline.coordinate(i, 2));
            Vec3 end = new Vec3(outline.coordinate(i, 3), outline.coordinate(i, 4), outline.coordinate(i, 5));
            Vec3 delta = end.subtract(start);
            double t = point.subtract(start).dot(delta) / delta.lengthSqr();
            if (t >= -1.0E-5 && t <= 1 + 1.0E-5 && start.add(delta.scale(t)).distanceToSqr(point) < 1.0E-10) return true;
        }
        return false;
    }
}
