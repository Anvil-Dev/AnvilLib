package dev.anvilcraft.lib.v2.recipe.init.recipe;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.trigger.IRecipeTrigger;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LibRecipeTriggers {
    public static final DeferredRegister<IRecipeTrigger> TRIGGER = DeferredRegister
        .create(LibRegistries.TRIGGER_REGISTRY, AnvilLibRecipe.MOD_ID);

    public static final DeferredHolder<IRecipeTrigger, IRecipeTrigger> ITEM_INTO_BLOCK = TRIGGER.register(
        "item_into_block",
        IRecipeTrigger.Impl::new
    );
}
