package dev.anvilcraft.lib.v2.rendering.foundation;

import com.mojang.blaze3d.vertex.CompactVectorArray;
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
        public int[] sort(CompactVectorArray points) {
            Vector3f vector3f = new Vector3f();
            int[] keys = new int[points.size()];
            int[] indices = new int[points.size()];

            for (int i = 0; i < points.size(); i++) {
                keys[i] = floatToSortableInt(origin.distanceSquared(points.get(i, vector3f)));
                indices[i] = i;
            }

            if (points.size() <= 128) {
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
