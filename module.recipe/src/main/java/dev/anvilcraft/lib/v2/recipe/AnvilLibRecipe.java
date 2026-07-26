package dev.anvilcraft.lib.v2.recipe;

import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.recipe.init.LibDataComponentPredicates;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeInits;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;

@Mod(AnvilLibRecipe.MOD_ID)
public class AnvilLibRecipe {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_recipe";
    public static final AnvilLibRecipeConfig CONFIG = ConfigManager.register(AnvilLibRecipe.MOD_ID, AnvilLibRecipeConfig::new);

    @ApiStatus.Internal
    public AnvilLibRecipe(IEventBus modEventBus, ModContainer modContainer) {
        LibDataComponentPredicates.initialize(modEventBus);
        LibRecipeInits.init(modEventBus);
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(MAIN_ID, path);
    }
}
