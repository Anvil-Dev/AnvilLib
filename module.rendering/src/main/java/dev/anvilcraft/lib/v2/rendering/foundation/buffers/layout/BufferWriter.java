package dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.nio.ByteBuffer;

public interface BufferWriter {
    void putMat4f(Matrix4f object);

    void putIVec4(Vector4i object);

    void putIVec3(Vector3i object);

    void putIVec2(Vector2i object);

    void putInt(int object);

    void putVec4(Vector4f object);

    void putVec3(Vector3f object);

    void putVec2(Vector2f object);

    void putVec2Array(int size, Vector2f[] vector2fs);

    void putIVec2Array(int size, Vector2i[] vector2is);

    void putFloat(float object);

    default ByteBuffer intoBuffer() {
        return this.intoBuffer(true);
    }

    ByteBuffer intoBuffer(boolean flip);

    <E> void putStructArray(int index, E object, BufferObjectLayoutDefinition<E> definition);

    <E> void putStructArray(E[] objects, int size, BufferObjectLayoutDefinition<E> definition);
}
