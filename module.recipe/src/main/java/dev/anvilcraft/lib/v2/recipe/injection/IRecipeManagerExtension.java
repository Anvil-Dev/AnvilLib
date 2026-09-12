package dev.anvilcraft.lib.v2.recipe.injection;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public interface IRecipeManagerExtension {
    default void anvillib$setInWorldRecipeManager(InWorldRecipeManager manager) {
        throw new AssertionError();
    }

    default InWorldRecipeManager anvillib$getInWorldRecipeManager() {
        throw new AssertionError();
    }

    @Deprecated
    default HolderLookup.Provider anvillib$getRegistries() {
        throw new AssertionError();
    }

    default void anvillib$addRecipes(List<RecipeHolder<InWorldRecipe>> recipes) {
        throw new AssertionError();
    }
}
