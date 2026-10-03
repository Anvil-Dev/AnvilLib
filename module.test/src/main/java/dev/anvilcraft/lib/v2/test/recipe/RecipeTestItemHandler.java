package dev.anvilcraft.lib.v2.test.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.items.ItemStackHandler;

class RecipeTestItemHandler extends ItemStackHandler {
    RecipeTestItemHandler(int size) {
        super(size);
    }

    static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    static ItemStack stack(ItemStack stack) {
        return stack.copy();
    }

    void set(int slot, ItemStack resource, int amount) {
        this.setStackInSlot(slot, resource.copyWithCount(amount));
    }

    int size() {
        return this.getSlots();
    }

    ItemStack getResource(int slot) {
        return this.getStackInSlot(slot);
    }

    int getAmountAsInt(int slot) {
        return this.getStackInSlot(slot).getCount();
    }
}
