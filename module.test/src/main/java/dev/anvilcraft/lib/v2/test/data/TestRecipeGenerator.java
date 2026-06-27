package dev.anvilcraft.lib.v2.test.data;

import dev.anvilcraft.lib.v2.recipe.builder.InWorldRecipeBuilder;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeTriggers;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.all.TestBlocks;
import dev.anvilcraft.lib.v2.test.all.TestItems;
import net.minecraft.resources.Identifier;

public class TestRecipeGenerator {
    public static final Identifier SUCCESS_RECIPE_ID = AnvilLibTest.of("recipe_success");
    public static final Identifier ROLLBACK_RECIPE_ID = AnvilLibTest.of("recipe_rollback");
    public static final Identifier PARTIAL_FAILURE_RECIPE_ID = AnvilLibTest.of("recipe_partial_failure");

    public static void accept(RegistrumRecipeProvider provider) {
        InWorldRecipeBuilder.compatible(LibRecipeTriggers.ITEM_INTO_BLOCK)
            .group("recipe_test")
            .hasBlock(TestBlocks.RECIPE_TARGET.get())
            .hasItemIngredient(builder -> builder.of(TestItems.RECIPE_INPUT.get()).count(1))
            .spawnItem(TestItems.RECIPE_OUTPUT.asStack())
            .save(provider, SUCCESS_RECIPE_ID);

        InWorldRecipeBuilder.compatible(LibRecipeTriggers.ITEM_INTO_BLOCK)
            .group("recipe_test")
            .hasBlock(TestBlocks.RECIPE_TARGET.get())
            .hasItemIngredient(builder -> builder.of(TestItems.RECIPE_INPUT.get()).count(1))
            .hasItem(builder -> builder.of(TestItems.RECIPE_OUTPUT.get()).moreThan(1))
            .spawnItem(TestItems.RECIPE_OUTPUT.asStack())
            .save(provider, ROLLBACK_RECIPE_ID);

        InWorldRecipeBuilder.compatible(LibRecipeTriggers.ITEM_INTO_BLOCK)
            .group("recipe_test")
            .hasBlock(TestBlocks.RECIPE_TARGET.get())
            .hasItemIngredient(builder -> builder.of(TestItems.RECIPE_INPUT.get()).count(1))
            .spawnItem(builder -> builder.item(TestItems.RECIPE_OUTPUT.get()).count(1.0f))
            .maxEfficiency(2)
            .save(provider, PARTIAL_FAILURE_RECIPE_ID);
    }
}
