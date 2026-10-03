package dev.anvilcraft.lib.v2.recipe.event;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
@ToString
@RequiredArgsConstructor
public class ItemCacheEvent extends Event {
    protected final ItemCache cache;

    /** Cancelling rejects one existing destination; the recipe can still spawn its output. */
    @Getter
    public static class SelectOutput extends ItemCacheEvent implements ICancellableEvent {
        private final ItemStack stack;
        private final Vec3 pos;
        private final Vec3 range;
        private final ICacheElement element;

        public SelectOutput(ItemCache cache, ItemStack stack, Vec3 pos, Vec3 range, ICacheElement element) {
            super(cache);
            this.stack = stack.copy();
            this.pos = pos;
            this.range = range;
            this.element = element;
        }

        public ItemStack getStack() {
            return this.stack.copy();
        }
    }

    @Getter
    @ToString
    public static class SpawnItemEntity extends ItemCacheEvent {
        private final ItemEntity entity;

        public SpawnItemEntity(ItemCache cache, ItemEntity entity) {
            super(cache);
            this.entity = entity;
        }
    }
}
