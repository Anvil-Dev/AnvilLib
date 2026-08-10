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

    void putFloat(float object);

    ByteBuffer intoBuffer();

    <E> void putStructArray(int index, E object, BufferObjectLayoutDefinition<E> definition);

    default  <E> void putStructArray(E[] objects, BufferObjectLayoutDefinition<E> definition) {
        for (int i = 0; i < objects.length; i++) {
            putStructArray(i, objects[i], definition);
        }
    }
}
