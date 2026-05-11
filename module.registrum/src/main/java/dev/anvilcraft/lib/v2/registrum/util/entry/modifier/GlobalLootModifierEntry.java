package dev.anvilcraft.lib.v2.registrum.util.entry.modifier;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;

public class GlobalLootModifierEntry<T extends IGlobalLootModifier> extends RegistryEntry<MapCodec<? extends IGlobalLootModifier>, MapCodec<T>> {
    public GlobalLootModifierEntry(AbstractRegistrum<?> owner, DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<T>> key) {
        super(owner, key);
    }

    public static <E extends IGlobalLootModifier> GlobalLootModifierEntry<E> cast(RegistryEntry<MapCodec<? extends IGlobalLootModifier>, MapCodec<E>> entry) {
        return RegistryEntry.cast(GlobalLootModifierEntry.class, entry);
    }
}
