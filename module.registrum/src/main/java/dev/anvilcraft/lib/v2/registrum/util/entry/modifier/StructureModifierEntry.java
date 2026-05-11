package dev.anvilcraft.lib.v2.registrum.util.entry.modifier;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.DeferredHolder;

public class StructureModifierEntry<T extends StructureModifier> extends RegistryEntry<MapCodec<? extends StructureModifier>, MapCodec<T>> {
    public StructureModifierEntry(AbstractRegistrum<?> owner, DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<T>> key) {
        super(owner, key);
    }

    public static <E extends StructureModifier> StructureModifierEntry<E> cast(RegistryEntry<MapCodec<? extends StructureModifier>, MapCodec<E>> entry) {
        return RegistryEntry.cast(StructureModifierEntry.class, entry);
    }
}
