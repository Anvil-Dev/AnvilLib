package dev.anvilcraft.lib.v2.registrum.util.entry;

import dev.anvilcraft.lib.v2.registrum.util.entry.recipe.RecipeSerializerEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.recipe.RecipeTypeEntry;
import net.minecraft.world.item.crafting.Recipe;

public record RecipeEntry<T extends Recipe<?>>(RecipeTypeEntry<T> type, RecipeSerializerEntry<T> serializer) {

}
