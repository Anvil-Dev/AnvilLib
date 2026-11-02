package dev.anvilcraft.lib.v2.test.data;

import dev.anvilcraft.lib.v2.recipe.builder.InWorldRecipeBuilder;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeTriggers;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

/** 可直接查看生成 JSON 的世界内配方示例。 */
public final class TestRecipeGenerator {
    public static void accept(RegistrumRecipeProvider provider) {
        InWorldRecipeBuilder.compatible(LibRecipeTriggers.ITEM_INTO_BLOCK)
            .group("examples")
            .hasBlockIngredient(input -> input.of(Blocks.CUT_COPPER_STAIRS.weathering().unaffected()))
            .setBlock(output -> output.block(Blocks.CUT_COPPER_STAIRS.waxed().unaffected()).copyProperties())
            .save(provider, AnvilLibTest.of("copy_block_properties"));
        InWorldRecipeBuilder.compatible(LibRecipeTriggers.ITEM_INTO_BLOCK)
            .group("examples")
            .hasBlockIngredient(input -> input.of(Blocks.OAK_STAIRS))
            .setBlock(output -> output.block(Blocks.OAK_LOG).mapProperty("facing", "axis",
                Map.of("north", "z", "south", "z", "east", "x", "west", "x")))
            .save(provider, AnvilLibTest.of("map_block_properties"));
    }
}
