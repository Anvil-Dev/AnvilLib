package dev.anvilcraft.lib.v2.test.data;

import dev.anvilcraft.lib.v2.recipe.builder.InWorldRecipeBuilder;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeTriggers;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.all.TestItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** 覆盖可重新加载的配方、进度与数值提供器数据生成。 */
public final class TestDataGenerator {
    private TestDataGenerator() {
    }

    public static void register() {
        AnvilLibTest.REGISTRUM.addDataGenerator(ProviderType.RECIPE, provider -> {
            ShapelessRecipeBuilder.shapeless(provider.getItems(), RecipeCategory.MISC, TestItems.TEST_ITEM.get())
                .requires(Items.CARROT)
                .unlockedBy("has_carrot", provider.has(Items.CARROT))
                .save(provider, ResourceKey.create(Registries.RECIPE, AnvilLibTest.of("test_item")));
            InWorldRecipeBuilder.incompatible(LibRecipeTriggers.ITEM_INTO_BLOCK)
                .icon(new ItemStackTemplate(TestItems.TEST_ITEM.get(), 1))
                .hasItemIngredient(Items.CARROT)
                .hasBlock(Blocks.DIAMOND_BLOCK)
                .spawnItem(new ItemStackTemplate(TestItems.TEST_ITEM.get(), 1))
                .group("port")
                .unlockedBy("has_carrot", provider.has(Items.CARROT))
                .save(provider, AnvilLibTest.of("test_in_world"));
        });
        AnvilLibTest.REGISTRUM.addDataGenerator(ProviderType.ADVANCEMENT, provider -> provider.accept(
            Advancement.Builder.advancement()
                .addCriterion("manual", new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance()))
                .build(AnvilLibTest.of("port/data_generation"))
        ));
    }
}
