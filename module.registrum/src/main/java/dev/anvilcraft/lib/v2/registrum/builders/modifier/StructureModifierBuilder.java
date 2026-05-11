package dev.anvilcraft.lib.v2.registrum.builders.modifier;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.modifier.GlobalLootModifierEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.modifier.StructureModifierEntry;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class StructureModifierBuilder<T extends StructureModifier, P> extends AbstractBuilder<MapCodec<? extends StructureModifier>, MapCodec<T>, P, StructureModifierBuilder<T, P>> {

    final MapCodec<T> codec;

    public StructureModifierBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, MapCodec<T> codec) {
        super(owner, parent, name, callback, NeoForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS);
        this.codec = codec;
    }

    @Override
    protected MapCodec<T> createEntry() {
        return codec;
    }

    @Override
    public StructureModifierEntry<T> register() {
        return (StructureModifierEntry<T>) super.register();
    }

    @Override
    protected StructureModifierEntry<T> createEntryWrapper(DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<T>> delegate) {
        return new StructureModifierEntry<>(getOwner(), delegate);
    }
}
