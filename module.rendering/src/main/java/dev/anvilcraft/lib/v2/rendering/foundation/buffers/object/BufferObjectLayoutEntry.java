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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public record BufferObjectLayoutEntry<T, I>(BufferObjectLayoutEntryType<T> type, Function<I, T> getter) {

    void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
        type.acceptSizeCalculator(sizeCalculator);
    }

    void acceptWriter(BufferWriter writer, I object) {
        type.acceptWriter(writer, getter.apply(object));
    }

    int alignment(BufferLayout layout) {
        return type.alignment(layout);
    }

    public static <T1, I1> Builder<T1, I1> builder(BufferObjectLayoutEntryType<T1> type) {
        return new Builder<>(type);
    }

    public static <I1> Builder<Integer, I1> ofInt() {
        return builder(BufferObjectLayoutEntryType.INT);
    }

    public static <I1> Builder<Float, I1> ofFloat() {
        return builder(BufferObjectLayoutEntryType.FLOAT);
    }

    public static <I1> Builder<Vector2f, I1> ofVec2f() {
        return builder(BufferObjectLayoutEntryType.VEC2);
    }

    public static <I1> Builder<Vector3f, I1> ofVec3f() {
        return builder(BufferObjectLayoutEntryType.VEC3);
    }

    public static <I1> Builder<Vector4f, I1> ofVec4f() {
        return builder(BufferObjectLayoutEntryType.VEC4);
    }

    public static <I1> Builder<Vector2i, I1> ofVec2i() {
        return builder(BufferObjectLayoutEntryType.IVEC2);
    }

    public static <I1> Builder<Vector3i, I1> ofVec3i() {
        return builder(BufferObjectLayoutEntryType.IVEC3);
    }

    public static <I1> Builder<Vector4i, I1> ofVec4i() {
        return builder(BufferObjectLayoutEntryType.IVEC4);
    }

    public static <I1> Builder<Matrix4f, I1> ofMat4f() {
        return builder(BufferObjectLayoutEntryType.MAT4);
    }

    public static class Builder<T, I> {
        private final BufferObjectLayoutEntryType<T> type;
        private Function<I, T> getter;
        private BiConsumer<I, T> setter;

        public Builder(BufferObjectLayoutEntryType<T> type) {
            this.type = type;
        }

        public Builder<T, I> forGetter(Function<I, T> getter) {
            this.getter = getter;
            return this;
        }

        public Builder<T, I> forSetter(BiConsumer<I, T> setter) {
            this.setter = setter;
            return this;
        }

        public BufferObjectLayoutEntry<T, I> build() {
            return new BufferObjectLayoutEntry<>(type, getter);
        }
    }
}
