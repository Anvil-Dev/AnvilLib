package dev.anvilcraft.lib.v2.recipe;

import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;

@Config(group = AnvilLibRecipe.MAIN_ID, name = AnvilLibRecipe.MOD_ID)
public class AnvilLibRecipeConfig {
    @Comment("Maximum efficiency of in world recipes")
    @BoundedDiscrete(min = 1, max = 128)
    public int inWorldRecipeMaxEfficiency = 64;
}
