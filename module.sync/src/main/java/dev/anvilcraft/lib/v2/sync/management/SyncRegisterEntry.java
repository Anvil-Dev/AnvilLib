package dev.anvilcraft.lib.v2.sync.management;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public record SyncRegisterEntry<T, ID>(
    Class<?> clazz,
    StreamCodec<ByteBuf, ID> idCodec,
    Function<T, ID> idGetter,
    Finder<T, ID> finder,
    boolean dimension,
    @Nullable Function<T, ResourceKey<Level>> dimensionGetter
) {
    public static <T, ID> SyncRegisterEntry<T, ID> create(
        Class<T> type,
        StreamCodec<ByteBuf, ID> idCodec,
        Function<T, ID> idGetter,
        Finder<T, ID> finder
    ) {
        return SyncRegisterEntry.create(type, idCodec, idGetter, finder, false, null);
    }

    @SuppressWarnings("resource")
    public static <T, ID> SyncRegisterEntry<T, ID> create(
        Class<T> type,
        StreamCodec<ByteBuf, ID> idCodec,
        Function<T, ID> idGetter,
        Finder<T, ID> finder,
        Function<T, Level> dimensionGetter
    ) {
        return SyncRegisterEntry.create(
            type, idCodec, idGetter, finder, true, t -> {
                Level apply = dimensionGetter.apply(t);
                if (apply == null) return null;
                return apply.dimension();
            }
        );
    }

    public static <T, ID> SyncRegisterEntry<T, ID> create(
        Class<T> type,
        StreamCodec<ByteBuf, ID> idCodec,
        Function<T, ID> idGetter,
        Finder<T, ID> finder,
        boolean dimension,
        @Nullable Function<T, ResourceKey<Level>> dimensionGetter
    ) {
        return new SyncRegisterEntry<>(type, idCodec, idGetter, finder, dimension, dimensionGetter);
    }

    @FunctionalInterface
    public interface Finder<T, ID> {
        @Nullable
        T apply(IPayloadContext context, ID id) throws Exception;
    }
}