package dev.anvilcraft.lib.v2.recipe.init;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.function.IOutcomeFunction;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.function.IPredicateFunction;
import dev.anvilcraft.lib.v2.recipe.trigger.IRecipeTrigger;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = AnvilLibRecipe.MOD_ID)
public class LibRegistries {
    public static final ResourceKey<Registry<IRecipeTrigger>> TRIGGER_KEY = ResourceKey.createRegistryKey(
        AnvilLibRecipe.of("trigger")
    );
    public static final Registry<IRecipeTrigger> TRIGGER_REGISTRY = new RegistryBuilder<>(TRIGGER_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<IRecipePredicate.Type<?>>> PREDICATE_KEY = ResourceKey.createRegistryKey(
        AnvilLibRecipe.of("predicate")
    );
    public static final Registry<IRecipePredicate.Type<?>> PREDICATE_TYPE_REGISTRY = new RegistryBuilder<>(PREDICATE_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<IPredicateFunction.Type<?>>> PREDICATE_FUNCTION_KEY = ResourceKey.createRegistryKey(
        AnvilLibRecipe.of("predicate_function")
    );
    public static final Registry<IPredicateFunction.Type<?>> PREDICATE_FUNCTION_TYPE_REGISTRY = new RegistryBuilder<>(PREDICATE_FUNCTION_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<IRecipeOutcome.Type<?>>> OUTCOME_KEY = ResourceKey.createRegistryKey(
        AnvilLibRecipe.of("outcome")
    );
    public static final Registry<IRecipeOutcome.Type<?>> OUTCOME_TYPE_REGISTRY = new RegistryBuilder<>(OUTCOME_KEY)
        .sync(true)
        .maxId(512)
        .create();

    public static final ResourceKey<Registry<IOutcomeFunction.Type<?>>> OUTCOME_FUNCTION_KEY = ResourceKey.createRegistryKey(
        AnvilLibRecipe.of("outcome_function")
    );
    public static final Registry<IOutcomeFunction.Type<?>> OUTCOM_FUNCTIONE_TYPE_REGISTRY = new RegistryBuilder<>(OUTCOME_FUNCTION_KEY)
        .sync(true)
        .maxId(512)
        .create();

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(TRIGGER_REGISTRY);
        event.register(PREDICATE_TYPE_REGISTRY);
        event.register(PREDICATE_FUNCTION_TYPE_REGISTRY);
        event.register(OUTCOME_TYPE_REGISTRY);
        event.register(OUTCOM_FUNCTIONE_TYPE_REGISTRY);
    }
}
