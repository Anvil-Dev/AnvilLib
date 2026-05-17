package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.SelfEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SelfBuilder<T, P, S extends SelfBuilder<T, P, S>> extends AbstractBuilder<T, T, P, S> {
    final NonNullSupplier<T> factory;
    public SelfBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, ResourceKey<? extends Registry<T>> registryType, NonNullSupplier<T> factory) {
        super(owner, parent, name, callback, registryType);
        this.factory = factory;
    }

    @Override
    protected T createEntry() {
        return factory.get();
    }

    @Override
    public SelfEntry<T> register() {
        return (SelfEntry<T>) super.register();
    }

    @Override
    protected SelfEntry<T> createEntryWrapper(DeferredHolder<T, T> delegate) {
        return new SelfEntry<>(getOwner(), delegate);
    }
}
