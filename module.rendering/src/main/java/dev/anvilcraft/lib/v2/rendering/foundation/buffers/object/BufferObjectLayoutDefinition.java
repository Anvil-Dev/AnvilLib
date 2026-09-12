package dev.anvilcraft.lib.v2.rendering.foundation.buffers.object;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferSizeCalculator;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferWriter;

import java.nio.ByteBuffer;

public class BufferObjectLayoutDefinition<T> {
    private final ImmutableList<BufferObjectLayoutEntry<?, T>> entries;

    public BufferObjectLayoutDefinition(ImmutableList<BufferObjectLayoutEntry<?, T>> entries) {
        this.entries = entries;
    }

    public ByteBuffer write(ByteBuffer buffer, T object, BufferLayout layout) {
        BufferWriter builder = layout.createWriter(buffer);
        this.writeInto(builder, object);
        return builder.intoBuffer();
    }

    public void writeInto(BufferWriter writer, T object) {
        for (BufferObjectLayoutEntry<?, T> entry : entries) {
            entry.acceptWriter(writer, object);
        }
    }

    public int size(BufferLayout layout) {
        BufferSizeCalculator sizeCalculator = layout.createSizeCalculator();
        for (BufferObjectLayoutEntry<?, T> entry : entries) {
            entry.acceptSizeCalculator(sizeCalculator);
        }
        return sizeCalculator.get();
    }

    public int alignment(BufferLayout layout) {
        int alignment = 1;
        for (BufferObjectLayoutEntry<?, T> entry : entries) {
            alignment = Math.max(alignment, entry.alignment(layout));
        }
        return layout == BufferLayout.STD140 ? Math.max(alignment, 16) : alignment;
    }

    @SafeVarargs
    public static <T> BufferObjectLayoutDefinition<T> create(BufferObjectLayoutEntry<?, T>... entries) {
        ImmutableList.Builder<BufferObjectLayoutEntry<?, T>> builder = ImmutableList.builder();
        builder.add(entries);
        return new BufferObjectLayoutDefinition<>(builder.build());
    }
}
