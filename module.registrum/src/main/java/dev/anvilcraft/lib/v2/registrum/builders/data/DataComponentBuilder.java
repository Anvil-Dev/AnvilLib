package dev.anvilcraft.lib.v2.registrum.builders.data;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.data.DataComponentEntry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;

public class DataComponentBuilder<E, P> extends AbstractBuilder<DataComponentType<?>, DataComponentType<E>, P, DataComponentBuilder<E, P>> {
    final DataComponentType.Builder<E> builder;
    public DataComponentBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback) {
        super(owner, parent, name, callback, Registries.DATA_COMPONENT_TYPE);
        builder = DataComponentType.builder();
    }

    public DataComponentBuilder<E, P> persistent(Codec<E> codec) {
        builder.persistent(codec);
        return this;
    }

    public DataComponentBuilder<E, P> networkSynchronized(StreamCodec<? super RegistryFriendlyByteBuf, E> streamCodec) {
        builder.networkSynchronized(streamCodec);
        return this;
    }

    public DataComponentBuilder<E, P> cacheEncoding() {
        builder.cacheEncoding();
        return this;
    }

    public DataComponentBuilder<E, P> ignoreSwapAnimation() {
        builder.ignoreSwapAnimation();
        return this;
    }

    @Override
    protected DataComponentType<E> createEntry() {
        return builder.build();
    }

    @Override
    public DataComponentEntry<E> register() {
        return (DataComponentEntry<E>) super.register();
    }

    @Override
    protected DataComponentEntry<E> createEntryWrapper(DeferredHolder<DataComponentType<?>, DataComponentType<E>> delegate) {
        return new DataComponentEntry<>(getOwner(), delegate);
    }
}
