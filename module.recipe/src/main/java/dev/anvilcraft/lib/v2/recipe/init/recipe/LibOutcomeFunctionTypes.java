package dev.anvilcraft.lib.v2.recipe.init.recipe;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.outcome.function.ApplyTagToComponent;
import dev.anvilcraft.lib.v2.recipe.outcome.function.IOutcomeFunction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LibOutcomeFunctionTypes {
    public static final DeferredRegister<IOutcomeFunction.Type<?>> OUTCOME_FUNCTION_TYPE = DeferredRegister
        .create(LibRegistries.OUTCOM_FUNCTIONE_TYPE_REGISTRY, AnvilLibRecipe.MOD_ID);

    public static final DeferredHolder<IOutcomeFunction.Type<?>, ApplyTagToComponent.Type> APPLY_TAG_TO_COMPONENT = OUTCOME_FUNCTION_TYPE
        .register("apply_tag_2_component", ApplyTagToComponent.Type::new);
}
