package dev.anvilcraft.lib.v2.registrum.util.entry.data;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class DataComponentEntry<E> extends RegistryEntry<DataComponentType<?>, DataComponentType<E>> {

    public DataComponentEntry(AbstractRegistrum<?> owner, DeferredHolder<DataComponentType<?>, DataComponentType<E>> key) {
        super(owner, key);
    }

    public static <E> DataComponentEntry<E> cast(RegistryEntry<DataComponentType<?>, DataComponentType<E>> entry) {
        return RegistryEntry.cast(DataComponentEntry.class, entry);
    }
}
