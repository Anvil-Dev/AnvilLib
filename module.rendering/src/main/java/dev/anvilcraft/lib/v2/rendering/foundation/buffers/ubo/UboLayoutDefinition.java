package dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;

import java.nio.ByteBuffer;

public class UboLayoutDefinition<T> {
    private final ImmutableList<UboLayoutEntry<?, T>> entries;

    public UboLayoutDefinition(ImmutableList<UboLayoutEntry<?, T>> entries) {
        this.entries = entries;
    }

    public ByteBuffer write(ByteBuffer buffer, T object) {
        Std140Builder builder = Std140Builder.intoBuffer(buffer);
        for (UboLayoutEntry<?, T> entry : entries) {
            entry.acceptWriter(builder, object);
        }
        return builder.get();
    }

    public int size() {
        Std140SizeCalculator sizeCalculator = new Std140SizeCalculator();
        for (UboLayoutEntry<?, T> entry : entries) {
            entry.acceptSizeCalculator(sizeCalculator);
        }
        return sizeCalculator.get();
    }

    @SafeVarargs
    public static <T> UboLayoutDefinition<T> create(UboLayoutEntry<?, T>... entries) {
        ImmutableList.Builder<UboLayoutEntry<?, T>> builder = ImmutableList.builder();
        builder.add(entries);
        return new UboLayoutDefinition<>(builder.build());
    }
}
