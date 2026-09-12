/*
 *
 *  * Original work copyright (c) 2019 tterrag1098 (Registrate)
 *  * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *  *
 *  * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/builders/NoConfigBuilder.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import dev.anvilcraft.lib.v2.util.nullness.NonnullType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class NoConfigBuilder<R, T extends R, P> extends AbstractBuilder<R, T, P, NoConfigBuilder<R, T, P>> {

    private final NonNullSupplier<T> factory;

    public NoConfigBuilder(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        ResourceKey<? extends Registry<R>> registryType,
        NonNullSupplier<T> factory
    ) {
        super(owner, parent, name, callback, registryType);
        this.factory = factory;
    }

    @Override
    protected @NonnullType T createEntry() {
        return factory.get();
    }
}
