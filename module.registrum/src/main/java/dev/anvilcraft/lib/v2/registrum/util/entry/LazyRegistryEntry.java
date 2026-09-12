/*
 *
 *  * Original work copyright (c) 2019 tterrag1098 (Registrate)
 *  * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *  *
 *  * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/entry/LazyRegistryEntry.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.util.entry;

import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;

import javax.annotation.Nullable;

public class LazyRegistryEntry<R, T extends R> implements NonNullSupplier<T> {

    @Nullable
    private NonNullSupplier<? extends RegistryEntry<R, T>> supplier;
    @Nullable
    private RegistryEntry<R, T> value;

    public LazyRegistryEntry(NonNullSupplier<? extends RegistryEntry<R, T>> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        NonNullSupplier<? extends RegistryEntry<R, T>> supplier = this.supplier;
        if (supplier != null) {
            this.value = supplier.get();
            this.supplier = null;
        }
        return this.value.get();
    }
}
