package dev.anvilcraft.lib.v2.recipe.cache.item;

import net.minecraft.world.item.ItemStack;

/** A physical reservation and its exact item components. */
public final class ItemConsumption {
    private final ICacheElement element;
    private final ItemStack stack;
    private final Runnable restoration;
    private boolean restored;

    public ItemConsumption(ICacheElement element, ItemStack stack, Runnable restoration) {
        this.element = element;
        this.stack = stack.copy();
        this.restoration = restoration;
    }

    public ICacheElement element() {
        return this.element;
    }

    public ItemStack stack() {
        return this.stack.copy();
    }

    public int count() {
        return this.stack.getCount();
    }

    public void restore() {
        if (this.restored) return;
        this.restoration.run();
        this.restored = true;
    }
}
