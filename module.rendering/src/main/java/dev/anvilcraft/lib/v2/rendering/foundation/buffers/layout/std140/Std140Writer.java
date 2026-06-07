package dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std140;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferWriter;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.nio.ByteBuffer;

public final class Std140Writer implements BufferWriter {

    private final ByteBuffer buffer;
    private final int pointer;
    private BufferObjectLayoutDefinition<?> indexedArrayDefinition;
    private int indexedArrayStart;
    private int indexedArrayStride;
    private int indexedArrayEnd;

    public Std140Writer(ByteBuffer buffer) {
        this.buffer = buffer;
        this.pointer = buffer.position();
    }

    public void align(int alignment) {
        int position = this.buffer.position();
        this.buffer.position(this.pointer + Mth.roundToward(position - this.pointer, alignment));
    }

    public void putFloat(float value) {
        this.align(4);
        this.buffer.putFloat(value);
    }

    public void putInt(int value) {
        this.align(4);
        this.buffer.putInt(value);
    }

    public void putVec2(Vector2f vec) {
        this.align(8);
        vec.get(this.buffer);
        this.buffer.position(this.buffer.position() + 8);
    }

    public void putIVec2(Vector2i vec) {
        this.align(8);
        vec.get(this.buffer);
        this.buffer.position(this.buffer.position() + 8);
    }

    public void putVec3(Vector3f vec) {
        this.align(16);
        vec.get(this.buffer);
        this.buffer.position(this.buffer.position() + 12);
    }

    public void putIVec3(Vector3i vec) {
        this.align(16);
        vec.get(this.buffer);
        this.buffer.position(this.buffer.position() + 12);
    }

    public void putVec4(Vector4f vec) {
        this.align(16);
        vec.get(this.buffer);
        this.buffer.position(this.buffer.position() + 16);
    }

    public void putIVec4(Vector4i vec) {
        this.align(16);
        vec.get(this.buffer);
        this.buffer.position(this.buffer.position() + 16);
    }

    public void putMat4f(Matrix4f vec) {
        this.align(16);
        vec.get(this.buffer);
        this.buffer.position(this.buffer.position() + 64);
    }

    public ByteBuffer intoBuffer() {
        return this.buffer.flip();
    }

    @Override
    public <E> void putStructArray(int index, E object, BufferObjectLayoutDefinition<E> definition) {
        int alignment = Mth.roundToward(definition.alignment(BufferLayout.STD140), 16);
        int stride = Mth.roundToward(definition.size(BufferLayout.STD140), alignment);
        int arrayStart = this.indexedArrayStart(definition, alignment, stride);
        int elementStart = arrayStart + stride * index;
        this.buffer.position(elementStart);
        definition.writeInto(this, object);
        this.indexedArrayEnd = Math.max(this.indexedArrayEnd, elementStart + stride);
        this.buffer.position(this.indexedArrayEnd);
    }

    @Override
    public <E> void putStructArray(E[] objects, BufferObjectLayoutDefinition<E> definition) {
        int alignment = Mth.roundToward(definition.alignment(BufferLayout.STD140), 16);
        int stride = Mth.roundToward(definition.size(BufferLayout.STD140), alignment);
        int arrayStart = this.pointer + Mth.roundToward(this.buffer.position() - this.pointer, alignment);
        for (int i = 0; i < objects.length; i++) {
            this.buffer.position(arrayStart + stride * i);
            definition.writeInto(this, objects[i]);
        }
        this.buffer.position(arrayStart + stride * objects.length);
        this.indexedArrayDefinition = null;
    }

    private int indexedArrayStart(BufferObjectLayoutDefinition<?> definition, int alignment, int stride) {
        int position = this.buffer.position();
        if (this.indexedArrayDefinition == definition
            && this.indexedArrayStride == stride
            && position >= this.indexedArrayStart
            && position <= this.indexedArrayEnd) {
            return this.indexedArrayStart;
        }

        this.indexedArrayDefinition = definition;
        this.indexedArrayStart = this.pointer + Mth.roundToward(position - this.pointer, alignment);
        this.indexedArrayStride = stride;
        this.indexedArrayEnd = this.indexedArrayStart;
        return this.indexedArrayStart;
    }
}
