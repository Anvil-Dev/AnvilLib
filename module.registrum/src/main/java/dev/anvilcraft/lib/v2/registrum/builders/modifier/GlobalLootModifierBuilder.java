package dev.anvilcraft.lib.v2.registrum.builders.modifier;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.modifier.GlobalLootModifierEntry;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class GlobalLootModifierBuilder<T extends IGlobalLootModifier, P> extends AbstractBuilder<MapCodec<? extends IGlobalLootModifier>, MapCodec<T>, P, GlobalLootModifierBuilder<T, P>> {

    final MapCodec<T> codec;

    public GlobalLootModifierBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, MapCodec<T> codec) {
        super(owner, parent, name, callback, NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS);
        this.codec = codec;
    }

    @Override
    protected MapCodec<T> createEntry() {
        return codec;
    }

    @Override
    public GlobalLootModifierEntry<T> register() {
        return (GlobalLootModifierEntry<T>) super.register();
    }

    @Override
    protected GlobalLootModifierEntry<T> createEntryWrapper(DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<T>> delegate) {
        return new GlobalLootModifierEntry<>(getOwner(), delegate);
    }
}
