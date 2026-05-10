package dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import org.joml.*;

public interface UboLayoutEntryType<T> {
    UboLayoutEntryType<Float> FLOAT = new UboLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
            sizeCalculator.putFloat();
        }

        @Override
        public void acceptWriter(Std140Builder writer, Float object) {
            writer.putFloat(object);
        }
    };

    UboLayoutEntryType<Vector2f> VEC2 = new UboLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
            sizeCalculator.putVec2();
        }

        @Override
        public void acceptWriter(Std140Builder writer, Vector2f object) {
            writer.putVec2(object);
        }
    };

    UboLayoutEntryType<Vector3f> VEC3 = new UboLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
            sizeCalculator.putVec3();
        }

        @Override
        public void acceptWriter(Std140Builder writer, Vector3f object) {
            writer.putVec3(object);
        }
    };

    UboLayoutEntryType<Vector4f> VEC4 = new UboLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
            sizeCalculator.putVec4();
        }

        @Override
        public void acceptWriter(Std140Builder writer, Vector4f object) {
            writer.putVec4(object);
        }
    };

    UboLayoutEntryType<Integer> INT = new UboLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
            sizeCalculator.putInt();
        }

        @Override
        public void acceptWriter(Std140Builder writer, Integer object) {
            writer.putInt(object);
        }
    };

    UboLayoutEntryType<Vector2i> IVEC2 = new UboLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
            sizeCalculator.putIVec2();
        }

        @Override
        public void acceptWriter(Std140Builder writer, Vector2i object) {
            writer.putIVec2(object);
        }
    };

    UboLayoutEntryType<Vector3i> IVEC3 = new UboLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
            sizeCalculator.putIVec3();
        }

        @Override
        public void acceptWriter(Std140Builder writer, Vector3i object) {
            writer.putIVec3(object);
        }
    };

    UboLayoutEntryType<Vector4i> IVEC4 = new UboLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
            sizeCalculator.putIVec4();
        }

        @Override
        public void acceptWriter(Std140Builder writer, Vector4i object) {
            writer.putIVec4(object);
        }
    };

    UboLayoutEntryType<Matrix4f> MAT4 = new UboLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
            sizeCalculator.putMat4f();
        }

        @Override
        public void acceptWriter(Std140Builder writer, Matrix4f object) {
            writer.putMat4f(object);
        }
    };

    void acceptSizeCalculator(Std140SizeCalculator sizeCalculator);

    void acceptWriter(Std140Builder writer, T object);
}
