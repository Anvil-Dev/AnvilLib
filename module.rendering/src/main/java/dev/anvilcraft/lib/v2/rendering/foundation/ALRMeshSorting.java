package dev.anvilcraft.lib.v2.rendering.foundation;

import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.joml.Vector3f;

public class ALRMeshSorting {

    public static VertexSorting byDistance(Vector3f point) {
        return new VertexSortingDistanceToPoint(point);
    }

    public static class VertexSortingDistanceToPoint implements VertexSorting {

        private final Vector3f origin;

        private VertexSortingDistanceToPoint(Vector3f origin) {
            this.origin = origin;
        }

        @Override
        public int[] sort(Vector3f[] points) {
            int[] keys = new int[points.length];
            int[] indices = new int[points.length];

            for (int i = 0; i < points.length; i++) {
                keys[i] = floatToSortableInt(origin.distanceSquared(points[i]));
                indices[i] = i;
            }

            if (points.length <= 128) {
                IntArrays.quickSortIndirect(indices, keys);
            } else {
                IntArrays.radixSortIndirect(indices, keys, true);
            }
            return indices;
        }
    }

    public static int floatToSortableInt(float f) {
        int b = Float.floatToRawIntBits(f);
        return b ^ ((b >> 31) | 0x7fffffff);
    }
}
