package dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std140;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferSizeCalculator;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import net.minecraft.util.Mth;

public final class Std140SizeCalculator implements BufferSizeCalculator {

    private int size;
    
    public Std140SizeCalculator() {
    }

    public void align(int alignment) {
        this.size = Mth.roundToward(this.size, alignment);
    }

    public void putFloat() {
        this.align(4);
        this.size += 4;
    }

    public void putInt() {
        this.align(4);
        this.size += 4;
    }

    public void putVec2() {
        this.align(8);
        this.size += 8;
    }

    public void putIVec2() {
        this.align(8);
        this.size += 8;
    }

    public void putVec3() {
        this.align(16);
        this.size += 12;
    }

    public void putIVec3() {
        this.align(16);
        this.size += 12;
    }

    public void putVec4() {
        this.align(16);
        this.size += 16;
    }

    public void putIVec4() {
        this.align(16);
        this.size += 16;
    }

    public void putMat4f() {
        this.align(16);
        this.size += 64;
    }

    @Override
    public void putStructArray(BufferObjectLayoutDefinition<?> definition, int size) {
        int alignment = Mth.roundToward(definition.alignment(BufferLayout.STD140), 16);
        int stride = Mth.roundToward(definition.size(BufferLayout.STD140), alignment);
        this.align(alignment);
        this.size += stride * size;
    }

    @Override
    public int get() {
        return size;
    }
}
