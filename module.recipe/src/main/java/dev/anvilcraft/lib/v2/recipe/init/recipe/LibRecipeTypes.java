package dev.anvilcraft.lib.v2.recipe.init.recipe;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

@SuppressWarnings("SameParameterValue")
public class LibRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, AnvilLibRecipe.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, AnvilLibRecipe.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<InWorldRecipe>> IN_WORLD_RECIPE =
        registerType("in_world_recipe");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InWorldRecipe>> IN_WORLD_RECIPE_SERIALIZER =
        RECIPE_SERIALIZERS.register(
            "in_world_recipe",
            () -> new RecipeSerializer<>(InWorldRecipe.Serializer.CODEC, InWorldRecipe.Serializer.STREAM_CODEC)
        );

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> registerType(String name) {
        return RECIPE_TYPES.register(
            name, () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return AnvilLibRecipe.of(name).toString();
                }
            }
        );
    }

    @ApiStatus.Internal
    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
