package dev.anvilcraft.lib.v2.recipe.event.listener;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeManagerEvent;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilLibRecipe.MOD_ID)
@ApiStatus.Internal
public class ResourceEventListener {
//    @SubscribeEvent
//    public static void onRecipeLoad(@NotNull RecipesUpdatedEvent event) {
//        ResourceEventListener.initManager(event.getRecipeManager());
//    }

    @SubscribeEvent
    public static void onServerStarted(@NotNull ServerStartedEvent event) {
        ResourceEventListener.initManager(event.getServer().getRecipeManager());
    }

    @SubscribeEvent
    public static void onDatapackSync(@NotNull OnDatapackSyncEvent event) {
        ResourceEventListener.initManager(event.getPlayerList().getServer().getRecipeManager());
    }

    public static void initManager(@NotNull RecipeManager manager) {
        InWorldRecipeManager manager1 = new InWorldRecipeManager();
        NeoForge.EVENT_BUS.post(new InWorldRecipeManagerEvent.Init(manager1, manager));
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> value = holder.value();
            if (!(value instanceof InWorldRecipe)) continue;
            //noinspection unchecked
            manager1.register((RecipeHolder<InWorldRecipe>) holder);
        }
        manager.anvillib$setInWorldRecipeManager(manager1);
    }
}
