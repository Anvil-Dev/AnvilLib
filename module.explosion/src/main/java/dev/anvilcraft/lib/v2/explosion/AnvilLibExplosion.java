package dev.anvilcraft.lib.v2.explosion;

import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.explosion.mixin.SingleItemRecipeAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(AnvilLibExplosion.MOD_ID)
@EventBusSubscriber(modid = AnvilLibExplosion.MOD_ID)
public class AnvilLibExplosion {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_explosion";
    public static final AnvilLibExplosionConfig CONFIG = ConfigManager.register(MOD_ID, AnvilLibExplosionConfig::new);
    public static final Map<Block, Block> MELTING_CACHE = new ConcurrentHashMap<>();

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibExplosion.MAIN_ID, path);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        AnvilLibExplosion.registerMelting(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        AnvilLibExplosion.registerMelting(event.getServer());
    }

    private static void registerMelting(MinecraftServer server) {
        AnvilLibExplosion.MELTING_CACHE.clear();
        BuiltInRegistries.BLOCK.get(BlockTags.LOGS_THAT_BURN).ifPresent(block -> {
            for (Holder<Block> blockHolder : block) {
                AnvilLibExplosion.MELTING_CACHE.put(blockHolder.value(), Blocks.COAL_BLOCK);
            }
        });
        AnvilLibExplosion.MELTING_CACHE.put(Blocks.GRASS_BLOCK, Blocks.PODZOL);
        AnvilLibExplosion.MELTING_CACHE.put(Blocks.MYCELIUM, Blocks.PODZOL);
        AnvilLibExplosion.MELTING_CACHE.put(Blocks.DIRT_PATH, Blocks.COARSE_DIRT);
        AnvilLibExplosion.MELTING_CACHE.put(Blocks.DIRT, Blocks.COARSE_DIRT);
        AnvilLibExplosion.MELTING_CACHE.put(Blocks.ROOTED_DIRT, Blocks.COARSE_DIRT);
        AnvilLibExplosion.MELTING_CACHE.put(Blocks.FARMLAND, Blocks.COARSE_DIRT);
        AnvilLibExplosion.MELTING_CACHE.put(Blocks.MUD, Blocks.DIRT);
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Recipe<?> value = holder.value();
            if (value instanceof SmeltingRecipe || value instanceof BlastingRecipe) {
                AnvilLibExplosion.registerAbstractCookingRecipe((AbstractCookingRecipe) value);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void registerAbstractCookingRecipe(AbstractCookingRecipe recipe) {
        Ingredient input = recipe.input();
        List<Item> items = input.items().map(Holder::value).toList();
        ItemStackTemplate result = ((SingleItemRecipeAccessor) recipe).getResult();
        if (result.count() != 1) {
            return;
        }
        if (!(result.item().value() instanceof BlockItem blockItemOutput)) {
            return;
        }
        for (Item item : items) {
            if (!(item instanceof BlockItem blockItemInput)) {
                continue;
            }
            Block blockInput = blockItemInput.getBlock();
            Block blockOutput = blockItemOutput.getBlock();
            MELTING_CACHE.put(blockInput, blockOutput);
        }
    }
}