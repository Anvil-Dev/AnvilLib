package dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;

public interface BufferSizeCalculator {
    void putFloat();

    void putVec2();

    void putVec3();

    void putVec4();

    void putInt();

    void putIVec2();

    void putIVec3();

    void putIVec4();

    void putMat4f();

    void putStructArray(BufferObjectLayoutDefinition<?> definition, int size);

    int get();
}
