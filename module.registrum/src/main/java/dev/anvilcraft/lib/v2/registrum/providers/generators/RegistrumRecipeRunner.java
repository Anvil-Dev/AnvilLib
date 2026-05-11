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
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.fml.LogicalSide;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class RegistrumRecipeRunner extends RecipeProvider.Runner implements RegistrumProvider {

    final AbstractRegistrum<?> owner;

    @Nullable
    RegistrumRecipeProvider provider;

    public RegistrumRecipeRunner(AbstractRegistrum<?> owner, PackOutput p_365369_, CompletableFuture<HolderLookup.Provider> p_361563_) {
        super(p_365369_, p_361563_);
        this.owner = owner;
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider p_362946_, RecipeOutput p_365274_) {
        return new RegistrumRecipeProvider(this, p_362946_, p_365274_);
    }

    @Override
    public String getName() {
        return "";
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
