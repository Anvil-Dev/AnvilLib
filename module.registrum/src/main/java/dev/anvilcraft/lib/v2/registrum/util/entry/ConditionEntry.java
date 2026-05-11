package dev.anvilcraft.lib.v2.registrum.util.entry;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ConditionEntry<T extends ICondition> extends RegistryEntry<MapCodec<? extends ICondition>, MapCodec<T>> {
    public ConditionEntry(AbstractRegistrum<?> owner, DeferredHolder<MapCodec<? extends ICondition>, MapCodec<T>> key) {
        super(owner, key);
    }

    public static <E extends ICondition> ConditionEntry<E> cast(RegistryEntry<MapCodec<? extends ICondition>, MapCodec<E>> entry) {
        return RegistryEntry.cast(ConditionEntry.class, entry);
    }
}
