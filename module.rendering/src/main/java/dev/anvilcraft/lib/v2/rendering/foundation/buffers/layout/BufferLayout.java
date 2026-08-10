package dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std140.Std140SizeCalculator;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std140.Std140Writer;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std430.Std430SizeCalculator;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std430.Std430Writer;

import java.nio.ByteBuffer;

public interface BufferLayout {
    BufferLayout STD140 = new BufferLayout() {
        @Override
        public BufferSizeCalculator createSizeCalculator() {
            return new Std140SizeCalculator();
        }

        @Override
        public BufferWriter createWriter(ByteBuffer buffer) {
            return new Std140Writer(buffer);
        }
    };

    BufferLayout STD430 = new BufferLayout() {
        @Override
        public BufferSizeCalculator createSizeCalculator() {
            return new Std430SizeCalculator();
        }

        @Override
        public BufferWriter createWriter(ByteBuffer buffer) {
            return new Std430Writer(buffer);
        }
    };

    BufferSizeCalculator createSizeCalculator();

    BufferWriter createWriter(ByteBuffer buffer);
}
