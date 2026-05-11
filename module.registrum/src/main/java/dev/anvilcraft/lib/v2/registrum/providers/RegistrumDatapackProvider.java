/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/RegistrateDatapackProvider.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RegistrumDatapackProvider extends DatapackBuiltinEntriesProvider implements RegistrumLookupFillerProvider {

    public RegistrumDatapackProvider(AbstractRegistrum<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(
            output,
            RegistryPatchGenerator.createLookup(provider, parent.getDataGenInitializer().getDatapackRegistryProviders()),
            Set.of(parent.getModid())
        );
    }

    @Override
    public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
        return getRegistryProvider();
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }

}
