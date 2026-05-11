package dev.anvilcraft.lib.v2.registrum.builders;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.ConditionEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.modifier.BiomeModifierEntry;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ConditionBuilder<T extends ICondition, P> extends AbstractBuilder<MapCodec<? extends ICondition>, MapCodec<T>, P, ConditionBuilder<T, P>> {
    private final MapCodec<T> mapCodec;
    public ConditionBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, MapCodec<T> mapCodec) {
        super(owner, parent, name, callback, NeoForgeRegistries.Keys.CONDITION_CODECS);
        this.mapCodec = mapCodec;
    }

    @Override
    protected MapCodec<T> createEntry() {
        return mapCodec;
    }

    @Override
    public ConditionEntry<T> register() {
        return (ConditionEntry<T>) super.register();
    }

    @Override
    protected ConditionEntry<T> createEntryWrapper(DeferredHolder<MapCodec<? extends ICondition>, MapCodec<T>> delegate) {
        return new ConditionEntry<>(getOwner(), delegate);
    }
}
