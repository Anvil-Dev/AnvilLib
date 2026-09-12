package dev.anvilcraft.lib.v2.recipe.init.recipe;

import net.neoforged.bus.api.IEventBus;

public class LibRecipeInits {
    public static void init(IEventBus modEventBus) {
        LibRecipeTriggers.TRIGGER.register(modEventBus);
        LibRecipePredicateTypes.PREDICATE_TYPE.register(modEventBus);
        LibPredicateFunctionTypes.PREDICATE_FUNCTION_TYPE.register(modEventBus);
        LibRecipeOutcomeTypes.OUTCOME_TYPE.register(modEventBus);
        LibOutcomeFunctionTypes.OUTCOME_FUNCTION_TYPE.register(modEventBus);
        LibRecipeTypes.register(modEventBus);
    }
}
