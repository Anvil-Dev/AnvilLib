package dev.anvilcraft.lib.registrar;

import net.minecraft.data.DataProvider;
import net.minecraft.data.models.ModelProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public interface DataProviderType<P extends DataProvider> {
    DataProviderType<ModelProvider> MODEL = new DataProviderType<>() {
    };
    DataProviderType<RecipeProvider> RECIPE = new DataProviderType<>() {
    };
    DataProviderType<TagsProvider<Item>> ITEM_TAG = new DataProviderType<>() {
    };
    DataProviderType<TagsProvider<Block>> BLOCK_TAG = new DataProviderType<>() {
    };
    DataProviderType<LanguageProvider> LANG = new DataProviderType<>() {
    };
}
