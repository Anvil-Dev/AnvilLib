package dev.anvilcraft.lib.v2.test.recipe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.IItemHandlerCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInput;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeTriggers;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.SpawnItem;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItem;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@GameTestHolder(AnvilLibTest.MOD_ID)
public final class RecipeRegressionTests {
    private static final BlockPos BASE = new BlockPos(3, 2, 3);
    private static final Vec3 RANGE = new Vec3(0.75, 0.75, 0.75);
    private static final ResourceLocation COMPONENT_PATH = AnvilLibTest.of("recipe_test_component");
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
        Map.entry("recipe_handler_rejects_unsupported_transfers", RecipeHandlerSyncTests::rejectsUnsupportedTransfers),
        Map.entry("recipe_failed_allocation_rolls_back", RecipeRegressionTests::failedAllocationRollsBack),
        Map.entry("recipe_shared_handler_catalyst", RecipeRegressionTests::sharedHandlerCatalyst),
        Map.entry("recipe_separate_handler_catalyst", RecipeRegressionTests::separateHandlerCatalyst),
        Map.entry("recipe_output_callback_runs_once", RecipeRegressionTests::outputCallbackRunsOnce),
        Map.entry("recipe_commit_is_idempotent", RecipeRegressionTests::commitIsIdempotent),
        Map.entry("recipe_components_follow_consumed_items", RecipeRegressionTests::componentsFollowConsumedItems),
        Map.entry("recipe_source_filter_accounts_exactly", RecipeRegressionTests::sourceFilterAccountsExactly),
        Map.entry("recipe_catalyst_reserves_without_consuming", RecipeRegressionTests::catalystReservesWithoutConsuming),
        Map.entry("recipe_legacy_presence_is_unchanged", RecipeRegressionTests::legacyPresenceIsUnchanged),
        Map.entry("recipe_legacy_json_and_stream_roundtrip", RecipeRegressionTests::legacyJsonAndStreamRoundtrip),
        Map.entry("recipe_queries_track_replaced_items", RecipeRegressionTests::queriesTrackReplacedItems),
        Map.entry("recipe_disjoint_scans_discover_gap", RecipeRegressionTests::disjointScansDiscoverGap),
        Map.entry("recipe_output_rejects_ordinary", RecipeOutputSelectionTests::rejectsOrdinaryEntity),
        Map.entry("recipe_output_merges_pending", RecipeOutputSelectionTests::mergesPendingOutput),
        Map.entry("recipe_output_filters_separate_keys", RecipeOutputSelectionTests::outputFiltersHaveSeparateKeys),
        Map.entry("recipe_batch_preserves_resources", RecipeContextTests::batchPreservesResources),
        Map.entry("recipe_commit_phases_are_stable", RecipeContextTests::commitPhasesAreStable),
        Map.entry("recipe_failed_commit_suppresses_effects", RecipeContextTests::failedCommitSuppressesEffects),
        Map.entry("recipe_failed_assembly_restores_reservations", RecipeContextTests::failedAssemblyRestoresReservations),
        Map.entry("recipe_matcher_exception_restores_reservations", RecipeContextTests::matcherExceptionRestoresReservations)
    );

    private RecipeRegressionTests() {
    }

    @GameTestGenerator
    public static List<TestFunction> generateTests() {
        if (!Boolean.getBoolean("anvillib.recipeTests")) return List.of();
        return TESTS.entrySet().stream().map(entry -> new TestFunction(
            "recipe_regression", AnvilLibTest.MOD_ID + ":" + entry.getKey(),
            AnvilLibTest.MOD_ID + ":recipe_empty", 100, 0, true, entry.getValue()
        )).toList();
    }
    private static void failedAllocationRollsBack(GameTestHelper helper) {
        TestItemHandler handler = new TestItemHandler(1);
        handler.set(0, TestItemHandler.stack(Items.IRON_INGOT), 9);
        Vec3 pos = addHandler(helper, handler, handler);
        InWorldRecipeContext context = context(helper, pos);
        ICacheInput input = context.computeIfAbsent(ItemCache.ITEM_CACHE).getInput(Items.IRON_INGOT, pos, RANGE);
        helper.assertTrue(input.shrink(5) == 0 && input.shrink(2) == 0, "The first two reservations fit");
        helper.assertTrue(input.rollbackShrink() == 2 && input.getCount() == 4, "Rollback releases the most recent two items");
        helper.assertTrue(input.rollbackShrink() == 5 && input.getCount() == 9, "Nested rollback restores the initial amount");
        InWorldRecipe recipe = recipe(List.of(ingredient(Items.IRON_INGOT, 5), ingredient(Items.IRON_INGOT, 2),
            ingredient(Items.IRON_INGOT, 5)), List.of(), List.of());
        helper.assertTrue(!recipe.matches(context, helper.getLevel()), "Nine items cannot satisfy [5, 2, 5]");
        helper.assertTrue(input.getCount() == 9 && context.getStack().isEmpty(), "Failed permutations restore every reservation");
        context.accept();
        helper.assertTrue(handler.getStackInSlot(0).getCount() == 9, "A failed match does not consume real items");
        helper.succeed();
    }

    private static void sharedHandlerCatalyst(GameTestHelper helper) {
        checkReturnedCatalyst(helper, true);
    }

    private static void separateHandlerCatalyst(GameTestHelper helper) {
        checkReturnedCatalyst(helper, false);
    }

    private static void checkReturnedCatalyst(GameTestHelper helper, boolean shared) {
        TestItemHandler input = new TestItemHandler(4);
        TestItemHandler output = shared ? input : new TestItemHandler(4);
        input.set(0, TestItemHandler.stack(Items.GOLD_INGOT), 1);
        input.set(1, TestItemHandler.stack(Items.IRON_INGOT), 1);
        Vec3 pos = addHandler(helper, input, output);
        InWorldRecipeContext context = context(helper, pos);
        InWorldRecipe recipe = recipe(List.of(ingredient(Items.GOLD_INGOT, 1), ingredient(Items.IRON_INGOT, 1)),
            List.of(), List.of(output(Items.DIAMOND, 3), output(Items.IRON_INGOT, 1)));
        execute(helper, recipe, context);
        context.accept();
        helper.assertTrue(count(input, output, Items.GOLD_INGOT) == 0, "The raw ingredient is consumed");
        helper.assertTrue(count(input, output, Items.IRON_INGOT) == 1, "A + B -> 3 C + B conserves exactly one catalyst");
        helper.assertTrue(count(input, output, Items.DIAMOND) == 3, "The recipe produces exactly three products");
        helper.succeed();
    }

    private static void outputCallbackRunsOnce(GameTestHelper helper) {
        DrainingHandler handler = new DrainingHandler();
        handler.set(0, TestItemHandler.stack(Items.IRON_INGOT), 1);
        Vec3 pos = addHandler(helper, handler, handler);
        ItemCache cache = new ItemCache(helper.getLevel());
        cache.getInput(Items.IRON_INGOT, pos, RANGE);
        cache.getOutput(new ItemStack(Items.IRON_INGOT), pos, new Vec3(0.05, 0.05, 0.05))
            .grow(new ItemStack(Items.IRON_INGOT), true);
        cache.getOutput(new ItemStack(Items.IRON_INGOT), pos, new Vec3(0.04, 0.04, 0.04))
            .grow(new ItemStack(Items.IRON_INGOT, 2), true);
        handler.armed = true;
        cache.endCache();
        helper.assertTrue(handler.delivered == 4 && handler.deliveries == 1,
            "Overlapping input/output queries synchronize a physical slot once");
        helper.assertTrue(handler.getStackInSlot(0).getCount() == 0, "Items taken by the commit callback are not written back");
        cache.endCache();
        helper.assertTrue(handler.delivered == 4 && handler.getStackInSlot(0).getCount() == 0,
            "Repeated cache finalization does not restore items removed by a callback");
        helper.succeed();
    }

    private static void commitIsIdempotent(GameTestHelper helper) {
        Vec3 pos = helper.absolutePos(BASE).getCenter();
        InWorldRecipeContext context = context(helper, pos);
        int[] callbacks = {0};
        context.putAcceptor(AnvilLibTest.of("recipe_test_acceptor"), ignored -> callbacks[0]++);
        output(Items.DIAMOND, 70).accept(context);
        context.accept();
        context.accept();
        context.get(ItemCache.ITEM_CACHE).endCache();
        int produced = itemEntities(helper, pos).stream().filter(entity -> entity.getItem().is(Items.DIAMOND))
            .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(produced == 70, "Repeated commit/finalization does not duplicate overflow drops");
        helper.assertTrue(callbacks[0] == 1, "An acceptor runs once for one commit");
        helper.succeed();
    }

    private static void componentsFollowConsumedItems(GameTestHelper helper) {
        TestItemHandler input = new TestItemHandler(3);
        TestItemHandler output = new TestItemHandler(4);
        input.set(0, TestItemHandler.stack(named(Items.APPLE, "alpha")), 1);
        input.set(1, TestItemHandler.stack(named(Items.APPLE, "beta")), 1);
        input.set(2, TestItemHandler.stack(Items.APPLE), 1);
        Vec3 pos = addHandler(helper, input, output);
        InWorldRecipeContext context = context(helper, pos);
        InWorldRecipe recipe = recipe(List.of(HasItemIngredient.builder().of(Items.APPLE).count(1).range(RANGE)
            .saveComponent(DataComponents.CUSTOM_NAME, COMPONENT_PATH).build()), List.of(), List.of(SpawnItem.builder()
            .item(named(Items.PAPER, "default"))
            .applyComponent(DataComponents.CUSTOM_NAME, COMPONENT_PATH).build()));
        for (int batch = 0; batch < 3; batch++) execute(helper, recipe, context);
        helper.assertTrue(!recipe.matches(context, helper.getLevel()), "Every successful batch consumes exactly one apple");
        context.accept();
        Map<String, Integer> names = new HashMap<>();
        for (int slot = 0; slot < output.getSlots(); slot++) {
            ItemStack stack = output.getStackInSlot(slot).copyWithCount(output.getStackInSlot(slot).getCount());
            if (stack.isEmpty()) continue;
            helper.assertTrue(stack.is(Items.PAPER), "Component forwarding retains the result item");
            names.merge(stack.getHoverName().getString(), stack.getCount(), Integer::sum);
        }
        helper.assertTrue(names.equals(Map.of("alpha", 1, "beta", 1, "default", 1)),
            "Each result uses only its consumed item's component; absent data retains the output default: " + names);
        helper.succeed();
    }

    private static void sourceFilterAccountsExactly(GameTestHelper helper) {
        Vec3 pos = helper.absolutePos(BASE).getCenter();
        ItemEntity processed = itemEntity(helper, pos, new ItemStack(Items.IRON_INGOT, 8));
        ItemEntity ordinary = itemEntity(helper, pos, new ItemStack(Items.IRON_INGOT, 64));
        ItemCache cache = new ItemCache(helper.getLevel());
        Predicate<ItemStack> item = stack -> stack.is(Items.IRON_INGOT);
        Predicate<ICacheElement> source = element -> element.getSource() == processed;
        helper.assertTrue(cache.getInput(item, pos, RANGE).getCount() == 72, "The unfiltered view contains both sources");
        ICacheInput filtered = cache.getInput(item, pos, RANGE, source);
        helper.assertTrue(filtered.getCount() == 8, "Ordinary items cannot make up the ninth processed item");
        helper.assertTrue(filtered.shrink(8) == 0, "Only eligible processed items are reserved");
        cache.endCache();
        helper.assertTrue(!processed.isAlive() || processed.getItem().isEmpty(), "The eligible source is consumed");
        helper.assertTrue(ordinary.isAlive() && ordinary.getItem().getCount() == 64,
            "Source filtering uses the same elements for counting and consumption");
        helper.succeed();
    }

    private static void queriesTrackReplacedItems(GameTestHelper helper) {
        TestItemHandler handler = new TestItemHandler(1);
        handler.set(0, TestItemHandler.stack(Items.IRON_INGOT), 1);
        Vec3 pos = addHandler(helper, handler, handler);
        ItemCache cache = new ItemCache(helper.getLevel());
        ICacheInput iron = cache.getInput(Items.IRON_INGOT, pos, RANGE);
        helper.assertTrue(iron.shrink(1) == 0, "The iron input is reserved");
        cache.getOutput(new ItemStack(Items.GOLD_INGOT), pos).grow(new ItemStack(Items.GOLD_INGOT), true);
        helper.assertTrue(iron.getCount() == 0, "A cached iron query cannot count the replacement gold");
        helper.assertTrue(cache.getInput(Items.GOLD_INGOT, pos, RANGE).getCount() == 1,
            "The replacement item remains available under its actual type");
        cache.endCache();
        helper.assertTrue(handler.getStackInSlot(0).is(Items.GOLD_INGOT) && handler.getStackInSlot(0).getCount() == 1,
            "A shared physical slot commits only its final item type");
        helper.succeed();
    }

    private static void disjointScansDiscoverGap(GameTestHelper helper) {
        Vec3 left = helper.absolutePos(new BlockPos(1, 2, 3)).getCenter();
        Vec3 middle = left.add(2, 0, 0);
        Vec3 right = left.add(4, 0, 0);
        itemEntity(helper, left, new ItemStack(Items.IRON_INGOT));
        itemEntity(helper, middle, new ItemStack(Items.IRON_INGOT));
        itemEntity(helper, right, new ItemStack(Items.IRON_INGOT));
        ItemCache cache = new ItemCache(helper.getLevel());
        helper.assertTrue(cache.getInput(Items.IRON_INGOT, left, RANGE).getCount() == 1, "The first area is scanned");
        helper.assertTrue(cache.getInput(Items.IRON_INGOT, right, RANGE).getCount() == 1, "The disjoint area is scanned");
        helper.assertTrue(cache.getInput(Items.IRON_INGOT, middle, RANGE).getCount() == 1,
            "The bounding box between scanned areas is not mistaken for an already scanned area");
        helper.succeed();
    }

    private static void catalystReservesWithoutConsuming(GameTestHelper helper) {
        TestItemHandler input = new TestItemHandler(2);
        TestItemHandler output = new TestItemHandler(2);
        input.set(0, TestItemHandler.stack(Items.IRON_INGOT), 1);
        Vec3 pos = addHandler(helper, input, output);
        HasItemIngredient catalyst = HasItemIngredient.builder().of(Items.IRON_INGOT).count(1).range(RANGE).consume(false).build();
        InWorldRecipe recipe = recipe(List.of(catalyst, ingredient(Items.IRON_INGOT, 1)),
            List.of(), List.of(output(Items.DIAMOND, 1)));
        InWorldRecipeContext insufficient = context(helper, pos);
        helper.assertTrue(!recipe.matches(insufficient, helper.getLevel()), "One item cannot be both reserved catalyst and consumed input");
        input.set(0, TestItemHandler.stack(Items.IRON_INGOT), 2);
        InWorldRecipeContext sufficient = context(helper, pos);
        execute(helper, recipe, sufficient);
        sufficient.accept();
        helper.assertTrue(input.getStackInSlot(0).getCount() == 1 && output.getStackInSlot(0).getCount() == 1,
            "Two items allow one input to be consumed while the reserved catalyst stays in its original slot");
        helper.succeed();
    }

    private static void legacyPresenceIsUnchanged(GameTestHelper helper) {
        TestItemHandler input = new TestItemHandler(1);
        TestItemHandler output = new TestItemHandler(1);
        input.set(0, TestItemHandler.stack(Items.IRON_INGOT), 1);
        Vec3 pos = addHandler(helper, input, output);
        InWorldRecipe recipe = recipe(List.of(ingredient(Items.IRON_INGOT, 1)),
            List.of(HasItem.builder().of(Items.IRON_INGOT).moreThan(1).range(RANGE).build()), List.of(output(Items.DIAMOND, 1)));
        InWorldRecipeContext context = context(helper, pos);
        execute(helper, recipe, context);
        context.accept();
        helper.assertTrue(input.getStackInSlot(0).getCount() == 0 && output.getStackInSlot(0).getCount() == 1,
            "Legacy HasItem remains a presence check and does not reserve a second item");
        helper.succeed();
    }

    private static void legacyJsonAndStreamRoundtrip(GameTestHelper helper) {
        var json = JsonParser.parseString("""
            {
              "trigger": "anvillib_recipe:item_into_block",
              "conflicting": [{"type": "anvillib_recipe:has_item_ingredient", "offset": [0, 0, 0],
                "range": [0.75, 0.75, 0.75], "item": {"items": "minecraft:iron_ingot", "count": 1}}],
              "non_conflicting": [],
              "outcomes": [{"type": "anvillib_recipe:spawn_item", "item": "minecraft:diamond", "offset": [0, 0, 0]}]
            }
            """);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        InWorldRecipe oldRecipe = new InWorldRecipe.Serializer().codec().codec().parse(ops, json).getOrThrow();
        var encoded = new InWorldRecipe.Serializer().codec().codec().encodeStart(ops, oldRecipe).getOrThrow();
        InWorldRecipe jsonRoundtrip = new InWorldRecipe.Serializer().codec().codec().parse(ops, encoded).getOrThrow();
        InWorldRecipe networkRoundtrip = streamRoundtrip(helper, jsonRoundtrip);
        TestItemHandler input = new TestItemHandler(1);
        TestItemHandler output = new TestItemHandler(1);
        input.set(0, TestItemHandler.stack(Items.IRON_INGOT), 1);
        Vec3 pos = addHandler(helper, input, output);
        InWorldRecipeContext context = context(helper, pos);
        execute(helper, networkRoundtrip, context);
        context.accept();
        helper.assertTrue(input.getStackInSlot(0).getCount() == 0 && output.getStackInSlot(0).getCount() == 1,
            "Legacy JSON without consume still consumes input after JSON and network roundtrips");
        InWorldRecipe catalystRecipe = recipe(List.of(HasItemIngredient.builder().of(Items.IRON_INGOT).count(1)
            .range(RANGE).consume(false).build()), List.of(), List.of(output(Items.DIAMOND, 1)));
        var catalystJson = new InWorldRecipe.Serializer().codec().codec().encodeStart(ops, catalystRecipe).getOrThrow();
        InWorldRecipe catalystRoundtrip = streamRoundtrip(helper,
            new InWorldRecipe.Serializer().codec().codec().parse(ops, catalystJson).getOrThrow());
        input.set(0, TestItemHandler.stack(Items.IRON_INGOT), 1);
        InWorldRecipeContext catalystContext = context(helper, pos);
        execute(helper, catalystRoundtrip, catalystContext);
        catalystContext.accept();
        helper.assertTrue(input.getStackInSlot(0).getCount() == 1 && output.getStackInSlot(0).getCount() == 2,
            "Explicit consume=false survives both serialization formats");
        helper.succeed();
    }

    private static InWorldRecipe streamRoundtrip(GameTestHelper helper, InWorldRecipe recipe) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            InWorldRecipe.Serializer.STREAM_CODEC.encode(buffer, recipe);
            InWorldRecipe decoded = InWorldRecipe.Serializer.STREAM_CODEC.decode(buffer);
            helper.assertTrue(!buffer.isReadable(), "The stream decoder consumes the full recipe payload");
            return decoded;
        } finally {
            buffer.release();
        }
    }

    static InWorldRecipe recipe(List<IRecipePredicate<?>> inputs, List<IRecipePredicate<?>> conditions,
                                List<IRecipeOutcome<?>> outputs) {
        return new InWorldRecipe(new ItemStack(Items.ANVIL), LibRecipeTriggers.ITEM_INTO_BLOCK.get(),
            inputs, conditions, outputs, 1, false, 1);
    }

    static HasItemIngredient ingredient(Item item, int count) {
        return HasItemIngredient.builder().of(item).count(count).range(RANGE).build();
    }

    private static SpawnItem output(Item item, int count) {
        return SpawnItem.builder().item(item).count(count).build();
    }

    private static void execute(GameTestHelper helper, InWorldRecipe recipe, InWorldRecipeContext context) {
        helper.assertTrue(recipe.matches(context, helper.getLevel()), "The recipe matches the supplied resources");
        recipe.assemble(context, helper.getLevel().registryAccess());
    }

    static InWorldRecipeContext context(GameTestHelper helper, Vec3 pos) {
        return new InWorldRecipeContext(helper.getLevel(), pos, null);
    }

    static Vec3 addHandler(GameTestHelper helper, IItemHandler input, IItemHandler output) {
        Vec3 pos = helper.absolutePos(BASE).getCenter();
        HandlerEntity entity = new HandlerEntity(helper.getLevel(), pos, input, output);
        helper.getLevel().addFreshEntity(entity);
        return entity.getBoundingBox().getCenter();
    }

    private static ItemEntity itemEntity(GameTestHelper helper, Vec3 pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, stack, 0, 0, 0);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static List<ItemEntity> itemEntities(GameTestHelper helper, Vec3 pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos).inflate(2));
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static int count(IItemHandler input, IItemHandler output, Item item) {
        int count = count(input, item);
        return input == output ? count : count + count(output, item);
    }

    private static int count(IItemHandler handler, Item item) {
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(item)) count += handler.getStackInSlot(slot).getCount();
        }
        return count;
    }

    private static final class HandlerEntity extends ArmorStand implements IItemHandlerCache {
        private final IItemHandler input;
        private final IItemHandler output;

        private HandlerEntity(ServerLevel level, Vec3 pos, IItemHandler input, IItemHandler output) {
            super(level, pos.x, pos.y, pos.z);
            this.input = input;
            this.output = output;
            this.setNoGravity(true);
        }

        @Override
        public IItemHandler getInput() {
            return this.input;
        }

        @Override
        public IItemHandler getOutput() {
            return this.output;
        }
    }

    private static final class DrainingHandler extends TestItemHandler {
        private boolean armed;
        private int delivered;
        private int deliveries;

        private DrainingHandler() {
            super(1);
        }

        @Override
        protected void onContentsChanged(int index) {
            int amount = this.getStackInSlot(index).getCount();
            if (!this.armed || amount == 0) return;
            this.delivered += amount;
            this.deliveries++;
            this.armed = false;
            this.set(index, ItemStack.EMPTY, 0);
            this.armed = true;
        }
    }
}
