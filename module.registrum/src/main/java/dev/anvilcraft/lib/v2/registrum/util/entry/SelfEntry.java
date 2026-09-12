package dev.anvilcraft.lib.v2.registrum.util.entry;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SelfEntry<T> extends RegistryEntry<T, T> {
    public SelfEntry(AbstractRegistrum<?> owner, DeferredHolder<T, T> key) {
        super(owner, key);
    }

    public static <T> SelfEntry<T> cast(RegistryEntry<T, T> entry) {
        return RegistryEntry.cast(SelfEntry.class, entry);
    }
}
