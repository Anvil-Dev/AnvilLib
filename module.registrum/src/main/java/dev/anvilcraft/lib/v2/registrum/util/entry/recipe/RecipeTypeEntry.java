package dev.anvilcraft.lib.v2.registrum.util.entry.recipe;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class RecipeTypeEntry<T extends Recipe<?>> extends RegistryEntry<RecipeType<?>, RecipeType<T>> {
    public RecipeTypeEntry(AbstractRegistrum<?> owner, DeferredHolder<RecipeType<?>, RecipeType<T>> key) {
        super(owner, key);
    }

    public static <T extends Recipe<?>> RecipeTypeEntry<T> cast(RegistryEntry<RecipeType<?>, RecipeType<T>> entry) {
        return RegistryEntry.cast(RecipeTypeEntry.class, entry);
    }
}
