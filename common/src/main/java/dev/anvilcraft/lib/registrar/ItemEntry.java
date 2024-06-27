package dev.anvilcraft.lib.registrar;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

public class ItemEntry<T extends Item> extends RegistryEntry<T> implements ItemLike {
    @Override
    public T get() {
        return null;
    }

    @Override
    public @NotNull Item asItem() {
        return this.get();
    }
}
