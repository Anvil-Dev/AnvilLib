package dev.anvilcraft.lib.v2.recipe.cache.item;

import net.neoforged.neoforge.items.IItemHandler;


/**
 * Optional contract for views that expose the same physical inventory through different handlers.
 * The returned handler must support synchronizing the physical slot, including reservation refunds.
 */
public interface ItemCacheSlotProvider {
    Slot getItemCacheSlot(int slot);

    record Slot(IItemHandler handler, int slot) {
    }
}
