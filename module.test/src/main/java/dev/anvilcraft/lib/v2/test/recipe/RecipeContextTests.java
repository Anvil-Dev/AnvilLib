package dev.anvilcraft.lib.v2.test.recipe;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.TagCache;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class RecipeContextTests {
    private RecipeContextTests() {
    }

    static void batchPreservesResources(GameTestHelper helper) {
        InWorldRecipeContext context = emptyContext(helper);
        ItemCache items = context.computeIfAbsent(ItemCache.ITEM_CACHE);
        TagCache tags = context.computeIfAbsent(TagCache.TAG_CACHE);
        tags.putTag(AnvilLibTest.of("previous_batch"), StringTag.valueOf("stale"));
        long before = context.getBatchId();
        context.beginBatch();
        RecipeRegressionTests.check(helper, context.getBatchId() == before + 1, "Each batch receives a new identifier");
        RecipeRegressionTests.check(helper, context.get(ItemCache.ITEM_CACHE) == items, "Batch boundaries retain the shared item reservation cache");
        RecipeRegressionTests.check(helper, context.get(TagCache.TAG_CACHE) == tags && tags.tags.isEmpty(), "Batch-local component data is cleared");
        context.beginBatch();
        RecipeRegressionTests.check(helper, context.getBatchId() == before + 2, "Batch identifiers continue increasing");
        IdentityKey first = new IdentityKey(1);
        IdentityKey equal = new IdentityKey(1);
        Object firstValue = context.computeByIdentity(first, Object::new);
        Object equalValue = context.computeByIdentity(equal, Object::new);
        RecipeRegressionTests.check(helper, firstValue != equalValue && firstValue == context.computeByIdentity(first, Object::new),
            "Equal predicate keys retain distinct cached views while repeated access to one key is stable");
        helper.succeed();
    }

    static void commitPhasesAreStable(GameTestHelper helper) {
        InWorldRecipeContext context = emptyContext(helper);
        List<String> order = new ArrayList<>();
        context.afterCommit(() -> order.add("effect"));
        context.putAcceptor(AnvilLibTest.of("z"), ignored -> order.add("z"));
        context.putAcceptor(BlockCache.BLOCK_CACHE.location(), ignored -> order.add("block"));
        context.putAcceptor(AnvilLibTest.of("middle"), 50, ignored -> order.add("middle"));
        context.putAcceptor(AnvilLibTest.of("a"), ignored -> order.add("a"));
        context.putAcceptor(ItemCache.ITEM_CACHE.location(), ignored -> order.add("item"));
        context.accept();
        context.accept();
        RecipeRegressionTests.check(helper, order.equals(List.of("item", "middle", "block", "a", "z", "effect")),
            "Resources commit in phase/key order before effects, and a second commit does not replay them: " + order);
        helper.succeed();
    }

    static void failedCommitSuppressesEffects(GameTestHelper helper) {
        InWorldRecipeContext context = emptyContext(helper);
        List<String> order = new ArrayList<>();
        context.putAcceptor(ItemCache.ITEM_CACHE.location(), ignored -> order.add("item"));
        context.putAcceptor(BlockCache.BLOCK_CACHE.location(), ignored -> {
            order.add("block");
            throw new IllegalStateException("Deliberate failed block commit");
        });
        context.putAcceptor(AnvilLibTest.of("late"), ignored -> order.add("late"));
        context.afterCommit(() -> order.add("effect"));
        expectFailure(helper, context::accept, "A resource failure is reported");
        expectFailure(helper, context::accept, "An invalidated context cannot retry a partial commit");
        RecipeRegressionTests.check(helper, order.equals(List.of("item", "block")), "Failed commits do not run later resources or success effects");
        helper.succeed();
    }

    static void failedAssemblyRestoresReservations(GameTestHelper helper) {
        RecipeTestItemHandler input = new RecipeTestItemHandler(1);
        input.set(0, RecipeTestItemHandler.stack(Items.IRON_INGOT), 3);
        Vec3 pos = RecipeRegressionTests.addHandler(helper, input, new RecipeTestItemHandler(1));
        InWorldRecipeContext context = RecipeRegressionTests.context(helper, pos);
        HasItemIngredient ingredient = RecipeRegressionTests.ingredient(Items.IRON_INGOT, 2);
        List<String> effects = new ArrayList<>();
        InWorldRecipe recipe = RecipeRegressionTests.recipe(List.of(ingredient), List.of(), List.of(new FailingOutcome(effects)));
        RecipeRegressionTests.check(helper, recipe.matches(context, helper.getLevel()), "The input can be reserved before assembly");
        RecipeRegressionTests.check(helper, ingredient.getItem(context).getCount() == 1, "The pending plan reserves two items");
        expectFailure(helper, () -> recipe.assemble(context), "An assembly failure is reported");
        RecipeRegressionTests.check(helper, ingredient.getItem(context).getCount() == 3 && context.getStack().isEmpty(),
            "A failed assembly rolls back its consumed-item reservations");
        expectFailure(helper, context::accept, "Failed assembly invalidates its context");
        RecipeRegressionTests.check(helper, input.getAmountAsInt(0) == 3 && effects.isEmpty(), "Failed assembly commits no items or success effects");
        helper.succeed();
    }

    static void matcherExceptionRestoresReservations(GameTestHelper helper) {
        RecipeTestItemHandler input = new RecipeTestItemHandler(1);
        input.set(0, RecipeTestItemHandler.stack(Items.IRON_INGOT), 5);
        Vec3 pos = RecipeRegressionTests.addHandler(helper, input, new RecipeTestItemHandler(1));
        InWorldRecipeContext context = RecipeRegressionTests.context(helper, pos);
        HasItemIngredient earlier = RecipeRegressionTests.ingredient(Items.IRON_INGOT, 1);
        context.push(earlier);
        HasItemIngredient current = RecipeRegressionTests.ingredient(Items.IRON_INGOT, 2);
        HasItemIngredient throwing = new HasItemIngredient(Vec3.ZERO, new Vec3(1, 1, 1),
            ItemIngredientPredicate.of(Items.IRON_INGOT).build(), List.of()) {
            @Override
            public boolean test(InWorldRecipeContext ignored) {
                throw new IllegalStateException("Deliberate predicate failure");
            }
        };
        InWorldRecipe recipe = RecipeRegressionTests.recipe(List.of(), List.of(current, throwing), List.of());
        expectFailure(helper, () -> recipe.matches(context, helper.getLevel()), "A predicate exception is reported");
        RecipeRegressionTests.check(helper, earlier.getItem(context).getCount() == 4 && context.getStack().equals(List.of(earlier)),
            "Matcher failure rolls back to the initial stack size without releasing earlier reservations");
        RecipeRegressionTests.check(helper, input.getAmountAsInt(0) == 5, "Matching never commits inventory changes");
        helper.succeed();
    }

    private static InWorldRecipeContext emptyContext(GameTestHelper helper) {
        return RecipeRegressionTests.context(helper, helper.absolutePos(new BlockPos(3, 2, 3)).getCenter());
    }

    private static void expectFailure(GameTestHelper helper, Runnable action, String message) {
        boolean failed = false;
        try {
            action.run();
        } catch (IllegalStateException exception) {
            failed = true;
        }
        RecipeRegressionTests.check(helper, failed, message);
    }

    private record FailingOutcome(List<String> effects) implements IRecipeOutcome<FailingOutcome> {
        @Override
        public void accept(InWorldRecipeContext context) {
            context.afterCommit(() -> this.effects.add("effect"));
            throw new IllegalStateException("Deliberate assembly failure");
        }

        @Override
        public IRecipeOutcome.Type<FailingOutcome> getType() {
            throw new UnsupportedOperationException("Test-only outcome has no serialized form");
        }
    }

    private record IdentityKey(int value) {
    }
}
