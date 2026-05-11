package dev.anvilcraft.lib.v2.registrum.builders.modifier;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.modifier.BiomeModifierEntry;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class BiomeModifierBuilder<T extends BiomeModifier, P> extends AbstractBuilder<MapCodec<? extends BiomeModifier>, MapCodec<T>, P, BiomeModifierBuilder<T, P>> {
    private final MapCodec<T> mapCodec;
    public BiomeModifierBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, MapCodec<T> mapCodec) {
        super(owner, parent, name, callback, NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS);
        this.mapCodec = mapCodec;
    }

    @Override
    protected MapCodec<T> createEntry() {
        return mapCodec;
    }

    @Override
    public BiomeModifierEntry<T> register() {
        return (BiomeModifierEntry<T>) super.register();
    }

    @Override
    protected BiomeModifierEntry<T> createEntryWrapper(DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<T>> delegate) {
        return new BiomeModifierEntry<>(getOwner(), delegate);
    }
}
