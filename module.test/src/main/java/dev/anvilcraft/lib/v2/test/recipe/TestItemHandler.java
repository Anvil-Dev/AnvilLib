package dev.anvilcraft.lib.v2.test.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Keeps the resource-count test fixtures equivalent across Minecraft inventory APIs. */
class TestItemHandler extends ItemStackHandler {
    TestItemHandler(int size) {
        super(size);
    }

    void set(int slot, ItemStack stack, int count) {
        this.setStackInSlot(slot, stack.copyWithCount(count));
    }

    static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    static ItemStack stack(ItemStack stack) {
        return stack;
    }
}