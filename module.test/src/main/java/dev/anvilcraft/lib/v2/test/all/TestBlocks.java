package dev.anvilcraft.lib.v2.test.all;

import com.mojang.math.Quadrant;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.block.TestBloomBlock;
import dev.anvilcraft.lib.v2.test.block.TestCachedRenderingBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

    public static final BlockEntry<TestCachedRenderingBlock> TEST_CACHED_RENDERING = AnvilLibTest.REGISTRUM
        .block("test_cached_rendering", TestCachedRenderingBlock::new)
        .properties(p -> p.noOcclusion().noCollision())
        .blockstate(() -> (ctx, provider) -> provider.blockStateOutput.accept(
            BlockModelGenerators.createSimpleBlock(
                ctx.get(),
                BlockModelGenerators.plainVariant(provider.withParent(ModelTemplates.SLAB_BOTTOM)
                    .texture(TextureSlot.BOTTOM, Identifier.withDefaultNamespace("block/dirt"), false)
                    .texture(TextureSlot.TOP, Identifier.withDefaultNamespace("block/dirt"), false)
                    .texture(TextureSlot.SIDE, Identifier.withDefaultNamespace("block/dirt"), false)
                    .build(ctx.get()))
            )
        ))
//        .blockstate(() -> (_, _) -> {
//            Identifier model = gen.withParent(ModelTemplates.SLAB_BOTTOM)
//                .texture(TextureSlot.BOTTOM, Identifier.withDefaultNamespace("block/dirt"), false)
//                .texture(TextureSlot.TOP, Identifier.withDefaultNamespace("block/dirt"), false)
//                .texture(TextureSlot.SIDE, Identifier.withDefaultNamespace("block/dirt"), false)
//                .build(ctx.get());
//
//            MultiVariantGenerator generator = MultiVariantGenerator.dispatch(ctx.get())
//                .with(PropertyDispatch.initial(TestCachedRenderingBlock.UP)
//                    .select(false, BlockModelGenerators.plainVariant(model))
//                    .select(true, BlockModelGenerators.plainVariant(model).with(VariantMutator.X_ROT.withValue(Quadrant.R180)))
//                );
//            gen.blockStateOutput.accept(generator);
//        })
        .simpleItem()
        .register();

    public static void setupRegistration() {
    }
}
