package dev.anvilcraft.lib.v2.test.recipe;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

final class RecipeHandlerSyncTests {
    private static final Vec3 RANGE = new Vec3(0.75, 0.75, 0.75);

    private RecipeHandlerSyncTests() {
    }

    static void rejectsUnsupportedTransfers(GameTestHelper helper) {
        GuardedHandler input = new GuardedHandler();
        GuardedHandler output = new GuardedHandler();
        input.contents.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
        input.rejectExtraction = true;
        Vec3 pos = RecipeRegressionTests.addHandler(helper, input, output);
        ItemCache extraction = new ItemCache(helper.getLevel());
        extraction.getInput(Items.IRON_INGOT, pos, RANGE).shrink(1);
        extraction.getOutput(new ItemStack(Items.DIAMOND), pos).grow(new ItemStack(Items.DIAMOND), true);
        expectRejected(helper, extraction);
        helper.assertTrue(input.getStackInSlot(0).getCount() == 2 && output.getStackInSlot(0).isEmpty(),
            "A handler that refuses extraction cannot produce a free output");

        input.contents.setStackInSlot(0, ItemStack.EMPTY);
        input.rejectExtraction = false;
        output.rejectInsertion = true;
        ItemCache insertion = new ItemCache(helper.getLevel());
        insertion.getOutput(new ItemStack(Items.DIAMOND), pos).grow(new ItemStack(Items.DIAMOND), true);
        expectRejected(helper, insertion);
        helper.assertTrue(output.getStackInSlot(0).isEmpty(), "Rejected insertion fails before any transfer");

        output.rejectInsertion = false;
        output.rejectActualInsertion = true;
        ItemCache dishonest = new ItemCache(helper.getLevel());
        dishonest.getOutput(new ItemStack(Items.DIAMOND), pos).grow(new ItemStack(Items.DIAMOND), true);
        expectRejected(helper, dishonest);
        helper.assertTrue(output.getStackInSlot(0).isEmpty(), "The actual remainder is checked even after a successful simulation");
        helper.succeed();
    }

    private static void expectRejected(GameTestHelper helper, ItemCache cache) {
        boolean failed = false;
        try {
            cache.endCache();
        } catch (IllegalStateException expected) {
            failed = true;
        }
        helper.assertTrue(failed, "Unsupported handler transfers must report failure");
        cache.endCache();
    }

    private static final class GuardedHandler implements IItemHandler {
        private final TestItemHandler contents = new TestItemHandler(1);
        private boolean rejectExtraction;
        private boolean rejectInsertion;
        private boolean rejectActualInsertion;

        @Override
        public int getSlots() {
            return this.contents.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.contents.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (this.rejectInsertion || (this.rejectActualInsertion && !simulate)) return stack.copy();
            return this.contents.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return this.rejectExtraction ? ItemStack.EMPTY : this.contents.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.contents.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return this.contents.isItemValid(slot, stack);
        }
    }
}
