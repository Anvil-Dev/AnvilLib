package dev.anvilcraft.lib.v2.recipe.cache.item;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Optional contract for views that expose the same physical inventory through different handlers.
 * The returned handler must support synchronizing the physical slot, including reservation refunds.
 */
public interface ItemCacheSlotProvider {
    Slot getItemCacheSlot(int slot);

    record Slot(ResourceHandler<ItemResource> handler, int slot) {
    }
}
