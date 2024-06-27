package dev.anvilcraft.lib.registrar;

import net.minecraft.data.models.ModelProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ItemBuilder<T extends Item> {
    private final ItemEntry<T> entry;
    private final AbstractRegistrar registrar;
    private final String name;
    private final Function<Item.Properties, T> factory;

    public ItemBuilder(AbstractRegistrar registrar, String name, Function<Item.Properties, T> factory) {
        this.registrar = registrar;
        this.factory = factory;
        this.name = name;
        this.entry = new ItemEntry<>();
    }

    public ItemBuilder<T> model(BiConsumer<ItemEntry<T>, ModelProvider> consumer) {
        this.registrar.data(DataProviderType.MODEL, p -> consumer.accept(this.entry, p));
        return this;
    }

    public ItemBuilder<T> tag(TagKey<Item>... tags) {
        return this;
    }

    public ItemBuilder<T> recipe(BiConsumer<ItemEntry<T>, RecipeProvider> consumer) {
        this.registrar.data(DataProviderType.RECIPE, p -> consumer.accept(this.entry, p));
        return this;
    }

    public ItemBuilder<T> lang(String name) {
        return this;
    }

    Item build() {
        return null;
    }

    public ItemEntry<T> register() {
        return null;
    }
}
