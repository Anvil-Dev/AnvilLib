package dev.anvilcraft.lib.v2.rendering.foundation.buffers.object;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferSizeCalculator;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferWriter;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

public interface BufferObjectLayoutEntryType<T> {
    BufferObjectLayoutEntryType<Float> FLOAT = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putFloat();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Float object) {
            writer.putFloat(object);
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 4;
        }
    };

    BufferObjectLayoutEntryType<Vector2f> VEC2 = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putVec2();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Vector2f object) {
            writer.putVec2(object);
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 8;
        }
    };

    BufferObjectLayoutEntryType<Vector3f> VEC3 = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putVec3();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Vector3f object) {
            writer.putVec3(object);
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 16;
        }
    };

    BufferObjectLayoutEntryType<Vector4f> VEC4 = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putVec4();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Vector4f object) {
            writer.putVec4(object);
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 16;
        }
    };

    BufferObjectLayoutEntryType<Integer> INT = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putInt();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Integer object) {
            writer.putInt(object);
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 4;
        }
    };

    BufferObjectLayoutEntryType<Vector2i> IVEC2 = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putIVec2();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Vector2i object) {
            writer.putIVec2(object);
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 8;
        }
    };

    BufferObjectLayoutEntryType<Vector3i> IVEC3 = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putIVec3();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Vector3i object) {
            writer.putIVec3(object);
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 16;
        }
    };

    BufferObjectLayoutEntryType<Vector4i> IVEC4 = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putIVec4();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Vector4i object) {
            writer.putIVec4(object);
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 16;
        }
    };

    BufferObjectLayoutEntryType<Matrix4f> MAT4 = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putMat4f();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Matrix4f object) {
            writer.putMat4f(object);
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 16;
        }
    };

    void acceptSizeCalculator(BufferSizeCalculator sizeCalculator);

    void acceptWriter(BufferWriter writer, T object);

    int alignment(BufferLayout layout);
}
