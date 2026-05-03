/*
 *
 *  * Original work copyright (c) 2019 tterrag1098 (Registrate)
 *  * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *  *
 *  * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/builders/BuilderCallback.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A callback passed to {@link Builder builders} from the owning {@link AbstractRegistrum} which will add a registration for the built entry that lazily creates and registers it.
 */
@FunctionalInterface
public interface BuilderCallback {

    /**
     * Accept a built entry, to later be constructed and registered.
     *
     * @param <R>          The registry type to which the entry will be registered
     * @param <T>          The type of the entry
     * @param name         The name of the entry
     * @param type         A {@link ResourceKey} representing the registry type
     * @param builder      The builder performing this callback
     * @param factory      A {@link NonNullSupplier} that will create the entry
     * @param entryFactory A {@link NonNullFunction} which accepts the entry delegate and returns a {@link RegistryEntry} wrapper
     * @return A {@link RegistryEntry} that will supply the registered entry
     */
    <R, T extends R> RegistryEntry<R, T> accept(
        String name,
        ResourceKey<? extends Registry<R>> type,
        Builder<R, T, ?, ?> builder,
        NonNullSupplier<? extends T> factory,
        NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory
    );

    /**
     * Accept a built entry, to later be constructed and registered. Uses the default {@link RegistryEntry#RegistryEntry(AbstractRegistrum, DeferredHolder) RegistryEntry factory}.
     *
     * @param <R>     The registry type to which the entry will be registered
     * @param <T>     The type of the entry
     * @param name    The name of the entry
     * @param type    A {@link Class} representing the registry type
     * @param builder The builder performing this callback
     * @param factory A {@link NonNullSupplier} that will create the entry
     * @return A {@link RegistryEntry} that will supply the registered entry
     */
    default <R, T extends R> RegistryEntry<R, T> accept(
        String name,
        ResourceKey<? extends Registry<R>> type,
        Builder<R, T, ?, ?> builder,
        NonNullSupplier<? extends T> factory
    ) {
        return accept(name, type, builder, factory, delegate -> new RegistryEntry<>(builder.getOwner(), delegate));
    }
}
