/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/entry/RegistryEntry.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.util.entry;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Wraps a {@link net.neoforged.neoforge.registries.DeferredHolder}, providing a cleaner API with null-safe access, and registrum-specific extensions such as {@link #getSibling(ResourceKey)}.
 *
 * @param <S> The type of the entry
 */
public class RegistryEntry<R, S extends R> extends DeferredHolder<R, S> implements NonNullSupplier<S> {
    private final AbstractRegistrum<?> owner;

    @SuppressWarnings("unused")
    public RegistryEntry(AbstractRegistrum<?> owner, DeferredHolder<R, S> key) {
        super(key.getKey());

        if (owner == null) {
            throw new NullPointerException("Owner must not be null");
        }
        this.owner = owner;
    }

    public <X, Y extends X> RegistryEntry<X, Y> getSibling(ResourceKey<? extends Registry<X>> registryType) {
        return owner.get(getId().getPath(), registryType);
    }

    public <X, Y extends X> RegistryEntry<X, Y> getSibling(Registry<X> registry) {
        return getSibling(registry.key());
    }

    /**
     * If an entry is present, and the entry matches the given predicate, return an {@link Optional<RegistryEntry>} describing the value, otherwise return an empty {@link Optional}.
     *
     * @param predicate a {@link Predicate predicate} to apply to the entry, if present
     * @return an {@link RegistryEntry} describing the value of this {@link RegistryEntry} if the entry is present and matches the given predicate, otherwise an empty {@link RegistryEntry}
     * @throws NullPointerException if the predicate is null
     */
    public Optional<RegistryEntry<R, S>> filter(Predicate<R> predicate) {
        Objects.requireNonNull(predicate);
        if (predicate.test(get())) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    public <X> boolean is(X entry) {
        return get() == entry;
    }

    @SuppressWarnings("unchecked")
    protected static <E extends RegistryEntry<?, ?>> E cast(Class<? super E> clazz, RegistryEntry<?, ?> entry) {
        if (clazz.isInstance(entry)) {
            return (E) entry;
        }
        throw new IllegalArgumentException("Could not convert RegistryEntry: expecting " + clazz + ", found " + entry.getClass());
    }
}
