package dev.anvilcraft.lib.v2.recipe.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.injection.IRecipeMapExtension;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(RecipeMap.class)
@ApiStatus.Internal
public class RecipeMapMixin implements IRecipeMapExtension {
    @Mutable
    @Final
    @Shadow
    private Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

    @Shadow private Multimap<RecipeType<?>, RecipeHolder<?>> byType;

    public void anvillib$addRecipes(List<RecipeHolder<InWorldRecipe>> recipes) {
        ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byNameBuilder = ImmutableMap.builder();
        Multimap<RecipeType<?>, RecipeHolder<?>> byTypeBuilder = MultimapBuilder.hashKeys().<RecipeHolder<?>>treeSetValues(
            Comparator.comparing(RecipeHolder::id)
        ).build();
        Set<Identifier> keys = new HashSet<>();
        this.byKey.forEach((key, value) -> {
            if (key == null || value == null) return;
            if (keys.contains(key.identifier())) return;
            keys.add(key.identifier());
            byNameBuilder.put(key, value);
        });
        this.byType.forEach((key, value) -> {
            if (key == null || value == null) return;
            byTypeBuilder.put(key, value);
        });
        recipes.forEach(recipe -> {
            if (keys.contains(recipe.id().identifier())) return;
            keys.add(recipe.id().identifier());
            byNameBuilder.put(recipe.id(), recipe);
            byTypeBuilder.put(recipe.value().getType(), recipe);
        });
        this.byKey = byNameBuilder.build();
        this.byType = ImmutableMultimap.copyOf(byTypeBuilder);
    }
}
