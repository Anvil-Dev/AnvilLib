package dev.anvilcraft.lib.v2.test.all;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.block.TestBloomBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.world.level.block.Blocks;

public class TestBlocks {
    static {
        AnvilLibTest.REGISTRUM.defaultCreativeTab(TestItemGroups.TEST_TAB);
    }

    public static final BlockEntry<TestBloomBlock> TEST_BLOOM = AnvilLibTest.REGISTRUM.block("test_bloom", TestBloomBlock::new)
        .properties(p -> p.noOcclusion().noCollision())
        .blockstate(() -> (ctx, provider) -> provider.blockStateOutput.accept(
            BlockModelGenerators.createSimpleBlock(
                ctx.get(),
                BlockModelGenerators.plainVariant(TexturedModel.CUBE.create(Blocks.COMMAND_BLOCK, provider.modelOutput))
            )
        ))
        .simpleItem()
        .register();

    public static void setupRegistration() {
    }
}
