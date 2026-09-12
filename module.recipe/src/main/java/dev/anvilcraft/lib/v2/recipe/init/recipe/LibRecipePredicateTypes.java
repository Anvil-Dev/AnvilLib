package dev.anvilcraft.lib.v2.recipe.init.recipe;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlock;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlockIngredient;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItem;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LibRecipePredicateTypes {
    public static final DeferredRegister<IRecipePredicate.Type<?>> PREDICATE_TYPE = DeferredRegister
        .create(LibRegistries.PREDICATE_TYPE_REGISTRY, AnvilLibRecipe.MOD_ID);

    public static final DeferredHolder<IRecipePredicate.Type<?>, HasItem.Type> HAS_ITEM = PREDICATE_TYPE.register(
        "has_item",
        HasItem.Type::new
    );

    public static final DeferredHolder<IRecipePredicate.Type<?>, HasItemIngredient.Type> HAS_ITEM_INGREDIENT = PREDICATE_TYPE.register(
        "has_item_ingredient",
        HasItemIngredient.Type::new
    );

    public static final DeferredHolder<IRecipePredicate.Type<?>, HasBlock.Type> HAS_BLOCK = PREDICATE_TYPE.register(
        "has_block",
        HasBlock.Type::new
    );

    public static final DeferredHolder<IRecipePredicate.Type<?>, HasBlockIngredient.Type> HAS_BLOCK_INGREDIENT = PREDICATE_TYPE.register(
        "has_block_ingredient",
        HasBlockIngredient.Type::new
    );
}
