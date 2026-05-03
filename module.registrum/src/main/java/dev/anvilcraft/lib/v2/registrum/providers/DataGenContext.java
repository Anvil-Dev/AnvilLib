/*
 *
 *  * Original work copyright (c) 2019 tterrag1098 (Registrate)
 *  * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *  *
 *  * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/DataGenContext.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.builders.Builder;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import dev.anvilcraft.lib.v2.util.nullness.NonnullType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Delegate;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

/**
 * A context bean passed to data generator callbacks. Contains the entry that data is being created for, and some metadata about the entry.
 *
 * @param <R> Type of the registry to which the entry belongs
 * @param <E> Type of the object for which data is being generated
 */
@Value
public class DataGenContext<R, E extends R> implements NonNullSupplier<E> {

    @Getter(AccessLevel.NONE)
    @Delegate
    NonNullSupplier<E> entry;
    String name;
    Identifier id;

    @SuppressWarnings("null")
    public @NonnullType E getEntry() {
        return entry.get();
    }

    @Deprecated
    public static <R, E extends R> DataGenContext<R, E> from(Builder<R, E, ?, ?> builder, ResourceKey<? extends Registry<R>> type) {
        return from(builder);
    }

    public static <R, E extends R> DataGenContext<R, E> from(Builder<R, E, ?, ?> builder) {
        return new DataGenContext<>(
            NonNullSupplier.of(builder.getOwner().<R, E>get(builder.getName(), builder.getRegistryKey())), builder.getName(),
            Identifier.fromNamespaceAndPath(builder.getOwner().getModid(), builder.getName())
        );
    }
}
