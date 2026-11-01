/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/generators/RegistrateRecipeRunner.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers.generators;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.Set;

@SuppressWarnings("unused")
public class RegistrumRecipeRunner implements RegistrumProvider {

    final AbstractRegistrum<?> owner;

    @Nullable
    RegistrumRecipeProvider provider;
    private final DatapackBuiltinEntriesProvider delegate;

    public RegistrumRecipeRunner(
        AbstractRegistrum<?> owner,
        PackOutput output,
        CompletableFuture<HolderLookup.Provider> worldRegistries,
        CompletableFuture<HolderLookup.Provider> reloadableRegistries
    ) {
        this.owner = owner;
        RegistrySetBuilder entries = new RegistrySetBuilder().add(
            RecipeProvider.asBootstrap((recipes, advancements) -> new RegistrumRecipeProvider(this, recipes, advancements))
        );
        this.delegate = DatapackBuiltinEntriesProvider.forReloadableLayer(
            output, "Registrum recipes", worldRegistries, reloadableRegistries, entries, Set.of(owner.getModid())
        );
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return this.delegate.run(cache);
    }

    @Override
    public String getName() {
        return "Recipes";
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }

    public RegistrumRecipeProvider getRecipeProvider() {
        if (provider == null) throw new IllegalStateException("Recipe Provider is not available now");
        return provider;
    }

}
