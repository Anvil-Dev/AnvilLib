package dev.anvilcraft.lib.v2.recipe.injection;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public interface IRecipeMapExtension {
    default void anvillib$addRecipes(List<RecipeHolder<InWorldRecipe>> recipes) {
        throw new AssertionError();
    }
}
