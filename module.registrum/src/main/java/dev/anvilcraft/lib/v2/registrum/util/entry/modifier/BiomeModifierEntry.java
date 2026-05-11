package dev.anvilcraft.lib.v2.registrum.util.entry.modifier;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BiomeModifierEntry<E extends BiomeModifier> extends RegistryEntry<MapCodec<? extends BiomeModifier>, MapCodec<E>> {
    public BiomeModifierEntry(AbstractRegistrum<?> owner, DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<E>> key) {
        super(owner, key);
    }

    public static <E extends BiomeModifier> BiomeModifierEntry<E> cast(RegistryEntry<MapCodec<? extends BiomeModifier>, MapCodec<E>> entry) {
        return RegistryEntry.cast(BiomeModifierEntry.class, entry);
    }
}
