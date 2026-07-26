package dev.anvilcraft.lib.v2.recipe.event.listener;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.event.ItemEntityEvent;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeTriggers;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilLibRecipe.MOD_ID)
@ApiStatus.Internal
public class ItemEntityEventListener {
    @SubscribeEvent
    public static void onItemEntityInToBlock(@NotNull ItemEntityEvent.InToBlock event) {
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        InWorldRecipeManager manager = serverLevel.recipeAccess().anvillib$getInWorldRecipeManager();
        InWorldRecipeContext context = new InWorldRecipeContext(serverLevel, event.getPos(), event.getEntity());
        manager.trigger(LibRecipeTriggers.ITEM_INTO_BLOCK, context);
        context.accept();
    }
}
