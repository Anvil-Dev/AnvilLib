package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.MobEffectEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MobEffectBuilder<T extends MobEffect, P> extends AbstractBuilder<MobEffect, T, P, MobEffectBuilder<T, P>> {
    final NonNullSupplier<T> supplier;
    public MobEffectBuilder(AbstractRegistrum<?> owner,
                            P parent,
                            String name,
                            BuilderCallback callback,
                            NonNullSupplier<T> supplier) {
        super(owner, parent, name, callback, Registries.MOB_EFFECT);
        this.supplier = supplier;
    }

    @Override
    public MobEffectEntry<T> register() {
        return (MobEffectEntry<T>) super.register();
    }

    @Override
    protected MobEffectEntry<T> createEntryWrapper(DeferredHolder<MobEffect, T> delegate) {
        return new MobEffectEntry<>(getOwner(), delegate);
    }

    @Override
    protected T createEntry() {
        return supplier.get();
    }
}
