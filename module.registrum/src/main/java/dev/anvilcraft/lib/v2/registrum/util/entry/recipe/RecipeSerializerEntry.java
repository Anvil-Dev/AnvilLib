package dev.anvilcraft.lib.v2.registrum.util.entry.recipe;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;

public class RecipeSerializerEntry<T extends Recipe<?>> extends RegistryEntry<RecipeSerializer<?>, RecipeSerializer<T>> {
    public RecipeSerializerEntry(AbstractRegistrum<?> owner, DeferredHolder<RecipeSerializer<?>, RecipeSerializer<T>> key) {
        super(owner, key);
    }

    public static <T extends Recipe<?>> RecipeSerializerEntry<T> cast(RegistryEntry<RecipeSerializer<?>, RecipeSerializer<T>> entry) {
        return RegistryEntry.cast(RecipeSerializerEntry.class, entry);
    }
}
