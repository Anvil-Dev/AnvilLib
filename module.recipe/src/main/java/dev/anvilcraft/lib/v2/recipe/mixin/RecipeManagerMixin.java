package dev.anvilcraft.lib.v2.recipe.mixin;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.injection.IRecipeManagerExtension;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin implements IRecipeManagerExtension {
    @Shadow
    @Final
    private RecipeMap recipes;
    @Unique
    private HolderLookup.Provider anvillib$registries;
    @Unique
    private InWorldRecipeManager anvillib$inWorldRecipeManager = null;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void anvillib$captureRegistries(HolderLookup.Provider registries, CallbackInfo ci) {
        this.anvillib$registries = registries;
    }

    @Override
    public void anvillib$setInWorldRecipeManager(InWorldRecipeManager manager) {
        this.anvillib$inWorldRecipeManager = manager;
    }

    @Override
    public InWorldRecipeManager anvillib$getInWorldRecipeManager() {
        return this.anvillib$inWorldRecipeManager;
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public HolderLookup.Provider anvillib$getRegistries() {
        return this.anvillib$registries;
    }

    @Override
    public void anvillib$addRecipes(List<RecipeHolder<InWorldRecipe>> recipes) {
        this.recipes.anvillib$addRecipes(recipes);
    }
}
