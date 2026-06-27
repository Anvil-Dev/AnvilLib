package dev.anvilcraft.lib.v2.test.recipe;

import dev.anvilcraft.lib.v2.recipe.event.ItemCacheEvent;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.all.TestBlocks;
import dev.anvilcraft.lib.v2.test.all.TestItems;
import dev.anvilcraft.lib.v2.test.data.TestRecipeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilLibTest.MOD_ID)
public final class RecipeIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeIntegrationTest.class);
    private static final ThreadLocal<List<ItemStack>> SPAWNED_OUTPUTS = ThreadLocal.withInitial(ArrayList::new);

    private RecipeIntegrationTest() {
    }

    @SubscribeEvent
    public static void onSpawn(ItemCacheEvent.SpawnItemEntity event) {
        SPAWNED_OUTPUTS.get().add(event.getEntity().getItem().copy());
    }

    public static void runAll(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos origin = BlockPos.containing(player.position()).above();
        List<String> failures = new ArrayList<>();

        checkNonNullManager(level, failures);
        runSuccessRecipe(level, origin, failures);
        runRollbackRecipe(level, origin.offset(0, 0, 4), failures);
        runPartialFailureRecipe(level, origin.offset(0, 0, 8), failures);

        if (failures.isEmpty()) {
            player.sendSystemMessage(Component.literal("Recipe integration tests passed."));
            LOGGER.info("Recipe integration tests passed");
            return;
        }

        player.sendSystemMessage(Component.literal("Recipe integration tests failed: " + failures.size()));
        for (String failure : failures) {
            player.sendSystemMessage(Component.literal(" - " + failure));
            LOGGER.error("Recipe integration test failure: {}", failure);
        }
    }

    private static void checkNonNullManager(ServerLevel level, List<String> failures) {
        InWorldRecipeManager manager = level.recipeAccess().anvillib$getInWorldRecipeManager();
        if (manager == null) {
            failures.add("recipe manager getter returned null");
        }
    }

    private static void runSuccessRecipe(ServerLevel level, BlockPos pos, List<String> failures) {
        resetArea(level, pos);
        level.setBlockAndUpdate(pos, TestBlocks.RECIPE_TARGET.get().defaultBlockState());
        spawnInput(level, pos, 1);
        clearCapturedOutputs();

        tickItemEntities(level, 3);

        assertBlock(level, pos, TestBlocks.RECIPE_TARGET.get().defaultBlockState(), "success recipe should keep target block", failures);
        assertNearbyCount(level, pos, TestItems.RECIPE_INPUT.asStack(), 0, "success recipe should consume input", failures);
        assertCapturedOutputCount(TestItems.RECIPE_OUTPUT.asStack(), 1, "success recipe should emit one output", failures);
    }

    private static void runRollbackRecipe(ServerLevel level, BlockPos pos, List<String> failures) {
        resetArea(level, pos);
        level.setBlockAndUpdate(pos, TestBlocks.RECIPE_TARGET.get().defaultBlockState());
        spawnInput(level, pos, 1);
        clearCapturedOutputs();

        tickItemEntities(level, 3);

        assertNearbyCount(level, pos, TestItems.RECIPE_INPUT.asStack(), 1, "rollback recipe should leave input untouched after compatible failure", failures);
        assertCapturedOutputCount(TestItems.RECIPE_OUTPUT.asStack(), 0, "rollback recipe should not emit output", failures);
    }

    private static void runPartialFailureRecipe(ServerLevel level, BlockPos pos, List<String> failures) {
        resetArea(level, pos);
        level.setBlockAndUpdate(pos, TestBlocks.RECIPE_TARGET.get().defaultBlockState());
        spawnInput(level, pos, 1);
        clearCapturedOutputs();

        tickItemEntities(level, 3);

        assertNearbyCount(level, pos, TestItems.RECIPE_INPUT.asStack(), 0, "partial failure recipe should still consume the single successful input", failures);
        assertCapturedOutputCount(TestItems.RECIPE_OUTPUT.asStack(), 1, "partial failure recipe should not leak extra output after failed second pass", failures);
    }

    private static void spawnInput(ServerLevel level, BlockPos pos, int count) {
        ItemStack stack = TestItems.RECIPE_INPUT.asStack();
        stack.setCount(count);
        ItemEntity entity = new ItemEntity(
            level,
            pos.getX() + 0.5,
            pos.getY() + 1.1,
            pos.getZ() + 0.5,
            stack,
            0,
            0,
            0
        );
        level.addFreshEntity(entity);
    }

    private static void tickItemEntities(ServerLevel level, int ticks) {
        for (int i = 0; i < ticks; i++) {
            List<ItemEntity> items = new ArrayList<>(level.getEntities(EntityType.ITEM, new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000), item -> true));
            for (ItemEntity item : items) {
                item.tick();
            }
        }
    }

    private static void resetArea(ServerLevel level, BlockPos pos) {
        for (ItemEntity entity : new ArrayList<>(level.getEntities(EntityType.ITEM, new AABB(pos).inflate(2), item -> true))) {
            entity.discard();
        }
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    private static void clearCapturedOutputs() {
        SPAWNED_OUTPUTS.get().clear();
    }

    private static void assertCapturedOutputCount(ItemStack expected, int count, String message, List<String> failures) {
        int actual = 0;
        for (ItemStack stack : SPAWNED_OUTPUTS.get()) {
            if (ItemStack.isSameItemSameComponents(stack, expected)) {
                actual += stack.getCount();
            }
        }
        if (actual != count) {
            failures.add(message + " (expected " + count + ", got " + actual + ")");
        }
    }

    private static void assertNearbyCount(ServerLevel level, BlockPos pos, ItemStack expected, int count, String message, List<String> failures) {
        int actual = 0;
        for (ItemEntity entity : level.getEntities(EntityType.ITEM, new AABB(pos).inflate(2), item -> true)) {
            ItemStack stack = entity.getItem();
            if (ItemStack.isSameItemSameComponents(stack, expected)) {
                actual += stack.getCount();
            }
        }
        if (actual != count) {
            failures.add(message + " (expected " + count + ", got " + actual + ")");
        }
    }

    private static void assertBlock(ServerLevel level, BlockPos pos, BlockState expected, String message, List<String> failures) {
        if (!level.getBlockState(pos).equals(expected)) {
            failures.add(message + " (expected " + expected + ", got " + level.getBlockState(pos) + ")");
        }
    }
}
