package dev.anvilcraft.lib.v2.recipe.event;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.Event;

/**
 * Published after resource commit and the preceding deferred outcomes of one execution.
 * The batch id identifies that execution; the context is shared across the complete plan,
 * so its tag cache is not a snapshot of the identified batch's components or outputs.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class InWorldRecipeCommittedEvent extends Event {
    private final RecipeType<? extends InWorldRecipe> recipeType;
    private final ResourceLocation id;
    private final InWorldRecipe recipe;
    private final InWorldRecipeContext context;
    private final long batchId;
}
