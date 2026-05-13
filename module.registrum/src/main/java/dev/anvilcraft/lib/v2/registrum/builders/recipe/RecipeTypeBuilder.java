package dev.anvilcraft.lib.v2.registrum.builders.recipe;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.recipe.RecipeTypeEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class RecipeTypeBuilder<T extends Recipe<?>, P> extends AbstractBuilder<RecipeType<?>, RecipeType<T>, P, RecipeTypeBuilder<T, P>> {
    public RecipeTypeBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback) {
        super(owner, parent, name, callback, Registries.RECIPE_TYPE);
    }

    @Override
    protected RecipeType<T> createEntry() {
        return new RecipeType<T>() {
            @Override
            public String toString() {
                return getOwner().getModid() + ":" + getName();
            }
        };
    }

    @Override
    public RecipeTypeEntry<T> register() {
        return (RecipeTypeEntry<T>) super.register();
    }

    @Override
    protected RecipeTypeEntry<T> createEntryWrapper(DeferredHolder<RecipeType<?>, RecipeType<T>> delegate) {
        return new RecipeTypeEntry<>(getOwner(), delegate);
    }
}
