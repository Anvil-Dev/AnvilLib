package dev.anvilcraft.lib.v2.test.recipe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.outcome.SetBlock;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlockIngredient;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/** 验证转换语义及原料移除后的状态继承。 */
final class RecipeBlockStateTests {
    private static final BlockPos BASE = new BlockPos(3, 2, 3);

    static void copyConsumedState(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BASE);
        BlockState source = Blocks.CUT_COPPER_STAIRS.weathering().unaffected().defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
            .setValue(BlockStateProperties.HALF, Half.TOP)
            .setValue(BlockStateProperties.WATERLOGGED, true);
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, source, 3);
        InWorldRecipeContext context = RecipeRegressionTests.context(helper, Vec3.atCenterOf(pos));
        convert(helper, context, Blocks.CUT_COPPER_STAIRS.weathering().unaffected().defaultBlockState(),
            SetBlock.builder().block(Blocks.CUT_COPPER_STAIRS.waxed().unaffected()).copyProperties().build());
        BlockState result = context.get(BlockCache.BLOCK_CACHE).getBlockState(pos);
        helper.assertTrue(result.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.WEST
            && result.getValue(BlockStateProperties.HALF) == Half.TOP
            && result.getValue(BlockStateProperties.WATERLOGGED), "Consumed stairs retain facing, half and water");
        context.accept();
        helper.assertTrue(helper.getLevel().getBlockState(pos).equals(result), "Converted state reaches the world");
        helper.succeed();
    }

    static void mappingPrecedence(GameTestHelper helper) {
        BlockState source = Blocks.OAK_STAIRS.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        SetBlock mapped = SetBlock.builder().block(Blocks.SPRUCE_STAIRS).copyProperties()
            .mapProperty(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HORIZONTAL_FACING,
                Map.of(Direction.NORTH, Direction.EAST)).build();
        helper.assertTrue(mapped.transform(source).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
            "Explicit mapping overrides copied properties");
        SetBlock axis = SetBlock.builder().block(Blocks.OAK_LOG)
            .mapProperty("facing", "axis", Map.of("north", "z")).build();
        helper.assertTrue(axis.transform(source).getValue(BlockStateProperties.AXIS) == Direction.Axis.Z,
            "Mapping converts between different properties");
        helper.assertTrue(axis.transform(source.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST))
            .getValue(BlockStateProperties.AXIS) == Direction.Axis.Y, "Unmapped values retain the target preset");
        SetBlock rename = SetBlock.builder().block(Blocks.OAK_STAIRS).mapProperty("facing", "facing").build();
        helper.assertTrue(rename.transform(source).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.NORTH,
            "An omitted value table copies values directly");
        helper.assertTrue(axis.transform(Blocks.STONE.defaultBlockState()).equals(Blocks.OAK_LOG.defaultBlockState()),
            "Missing source properties do not change the result");
        helper.succeed();
    }

    static void batchSnapshots(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BASE);
        helper.getLevel().setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
        InWorldRecipeContext context = RecipeRegressionTests.context(helper, Vec3.atCenterOf(pos));
        convert(helper, context, Blocks.OAK_LOG.defaultBlockState(), SetBlock.builder().block(Blocks.BIRCH_LOG)
            .mapProperty("axis", "axis", Map.of("y", "x")).build());
        convert(helper, context, Blocks.BIRCH_LOG.defaultBlockState(),
            SetBlock.builder().block(Blocks.SPRUCE_LOG).copyProperties().build());
        context.accept();
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.SPRUCE_LOG)
            && helper.getLevel().getBlockState(pos).getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
            "The second batch inherits the first batch output instead of the original world state");
        helper.succeed();
    }

    static void doubleBlock(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BASE);
        BlockState lower = Blocks.COPPER_DOOR.weathering().unaffected().defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
            .setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.RIGHT)
            .setValue(BlockStateProperties.OPEN, true);
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, lower, 2);
        helper.getLevel().setBlock(pos.above(), lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 2);
        InWorldRecipeContext context = RecipeRegressionTests.context(helper, Vec3.atCenterOf(pos));
        convert(helper, context, lower, SetBlock.builder().block(Blocks.COPPER_DOOR.waxed().unaffected()).copyProperties().build());
        context.accept();
        for (BlockPos part : List.of(pos, pos.above())) {
            BlockState state = helper.getLevel().getBlockState(part);
            helper.assertTrue(state.is(Blocks.COPPER_DOOR.waxed().unaffected())
                && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST
                && state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.RIGHT
                && state.getValue(BlockStateProperties.OPEN), "Both halves preserve their source properties");
        }
        helper.assertTrue(helper.getLevel().getBlockState(pos.above()).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
            == DoubleBlockHalf.UPPER, "The upper half remains upper");
        helper.succeed();
    }

    static void legacyAndCodecs(GameTestHelper helper) {
        SetBlock legacy = new SetBlock(Blocks.OAK_LOG.defaultBlockState(), new CompoundTag(), Vec3.ZERO,
            ConstantValue.exactly(1));
        helper.assertTrue(!legacy.copyProperties() && legacy.propertyMappings().isEmpty(), "Legacy constructor defaults remain unchanged");
        helper.assertTrue(legacy.transform(Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z))
            .getValue(BlockStateProperties.AXIS) == Direction.Axis.Y, "Legacy output remains the exact configured state");
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var codec = new SetBlock.Type().codec().codec();
        SetBlock decoded = codec.parse(ops, JsonParser.parseString("{\"block\":\"minecraft:oak_log\",\"offset\":[0,0,0]}"))
            .getOrThrow();
        helper.assertTrue(!decoded.copyProperties() && decoded.propertyMappings().isEmpty(), "Legacy JSON remains readable");
        SetBlock mapped = SetBlock.builder().block(Blocks.OAK_LOG).copyProperties()
            .mapProperty("facing", "axis", Map.of("north", "z")).build();
        SetBlock json = codec.parse(ops, codec.encodeStart(ops, mapped).getOrThrow()).getOrThrow();
        helper.assertTrue(json.copyProperties() && json.propertyMappings().equals(mapped.propertyMappings()), "JSON round trip retains mappings");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var stream = new SetBlock.Type().streamCodec();
            stream.encode(buffer, mapped);
            SetBlock network = stream.decode(buffer);
            helper.assertTrue(network.copyProperties() && network.propertyMappings().equals(mapped.propertyMappings())
                && !buffer.isReadable(), "Network round trip consumes exactly one outcome");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    static void invalidMappings(GameTestHelper helper) {
        expectFailure(helper, () -> SetBlock.builder().block(Blocks.OAK_LOG).mapProperty("facing", "missing").build());
        expectFailure(helper, () -> SetBlock.builder().block(Blocks.OAK_LOG).mapProperty("facing", "axis", Map.of("north", "invalid")).build());
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        expectFailure(helper, () -> new SetBlock.Type().codec().codec().parse(ops, JsonParser.parseString(
            "{\"block\":\"minecraft:oak_log\",\"offset\":[0,0,0],\"property_mappings\":{\"axis\":{\"source\":\"facing\",\"values\":{\"north\":\"invalid\"}}}}"
        )).getOrThrow());
        helper.succeed();
    }

    private static void expectFailure(GameTestHelper helper, Runnable action) {
        boolean failed = false;
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            failed = true;
        }
        helper.assertTrue(failed, "Invalid mappings must fail before executing a recipe");
    }

    private static void convert(GameTestHelper helper, InWorldRecipeContext context, BlockState source, SetBlock output) {
        InWorldRecipe recipe = RecipeRegressionTests.recipe(List.of(),
            List.of(HasBlockIngredient.builder().of(source.getBlock()).build()), List.of(output));
        helper.assertTrue(recipe.matches(context, helper.getLevel()), "Source block matches before consumption");
        recipe.assemble(context);
    }
}
