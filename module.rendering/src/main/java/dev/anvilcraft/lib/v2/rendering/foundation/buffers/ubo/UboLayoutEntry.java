package dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import org.joml.*;

import java.util.function.Function;

public record UboLayoutEntry<T, I>(UboLayoutEntryType<T> type, Function<I, T> getter) {

    void acceptSizeCalculator(Std140SizeCalculator sizeCalculator) {
        type.acceptSizeCalculator(sizeCalculator);
    }

    void acceptWriter(Std140Builder writer, I object) {
        type.acceptWriter(writer, getter.apply(object));
    }

    public static <T1, I1> Builder<T1, I1> builder(UboLayoutEntryType<T1> type) {
        return new Builder<>(type);
    }

    public static <I1> Builder<Integer, I1> ofInt() {
        return builder(UboLayoutEntryType.INT);
    }

    public static <I1> Builder<Float, I1> ofFloat() {
        return builder(UboLayoutEntryType.FLOAT);
    }

    public static <I1> Builder<Vector2f, I1> ofVec2f() {
        return builder(UboLayoutEntryType.VEC2);
    }

    public static <I1> Builder<Vector3f, I1> ofVec3f() {
        return builder(UboLayoutEntryType.VEC3);
    }

    public static <I1> Builder<Vector4f, I1> ofVec4f() {
        return builder(UboLayoutEntryType.VEC4);
    }

    public static <I1> Builder<Vector2i, I1> ofVec2i() {
        return builder(UboLayoutEntryType.IVEC2);
    }

    public static <I1> Builder<Vector3i, I1> ofVec3i() {
        return builder(UboLayoutEntryType.IVEC3);
    }

    public static <I1> Builder<Vector4i, I1> ofVec4i() {
        return builder(UboLayoutEntryType.IVEC4);
    }

    public static <I1> Builder<Matrix4f, I1> ofMat4f() {
        return builder(UboLayoutEntryType.MAT4);
    }

    public static class Builder<T, I> {
        private final UboLayoutEntryType<T> type;
        private Function<I, T> getter;

        public Builder(UboLayoutEntryType<T> type) {
            this.type = type;
        }

        public Builder<T, I> forGetter(Function<I, T> getter) {
            this.getter = getter;
            return this;
        }

        public UboLayoutEntry<T, I> build() {
            return new UboLayoutEntry<>(type, getter);
        }
    }
}
