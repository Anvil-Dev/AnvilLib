package dev.anvilcraft.lib.v2.recipe.init.recipe;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.outcome.ChooseOneOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.ProduceExplosion;
import dev.anvilcraft.lib.v2.recipe.outcome.SetBlock;
import dev.anvilcraft.lib.v2.recipe.outcome.SpawnItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LibRecipeOutcomeTypes {
    public static final DeferredRegister<IRecipeOutcome.Type<?>> OUTCOME_TYPE = DeferredRegister
        .create(LibRegistries.OUTCOME_TYPE_REGISTRY, AnvilLibRecipe.MOD_ID);

    public static final DeferredHolder<IRecipeOutcome.Type<?>, SpawnItem.Type> SPAWN_ITEM = OUTCOME_TYPE
        .register("spawn_item", SpawnItem.Type::new);

    public static final DeferredHolder<IRecipeOutcome.Type<?>, SetBlock.Type> SET_BLOCK = OUTCOME_TYPE
        .register("set_block", SetBlock.Type::new);

    public static final DeferredHolder<IRecipeOutcome.Type<?>, ProduceExplosion.Type> PRODUCE_EXPLOSION = OUTCOME_TYPE
        .register("produce_explosion", ProduceExplosion.Type::new);

    public static final DeferredHolder<IRecipeOutcome.Type<?>, ChooseOneOutcome.Type> CHOOSE_ONE = OUTCOME_TYPE
        .register("choose_one", ChooseOneOutcome.Type::new);
}
