package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.PotionEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;

public class PotionBuilder<P> extends AbstractBuilder<Potion, Potion, P, PotionBuilder<P>> {
    MobEffectInstance[] effects;
    public PotionBuilder(AbstractRegistrum<?> owner,
                         P parent,
                         String name,
                         BuilderCallback callback,
                         MobEffectInstance... effects) {
        super(owner, parent, name, callback, Registries.POTION);
        this.effects = effects;
    }

    @Override
    public PotionEntry register() {
        return (PotionEntry) super.register();
    }

    @Override
    protected PotionEntry createEntryWrapper(DeferredHolder<Potion, Potion> delegate) {
        return new PotionEntry(getOwner(), delegate);
    }

    @Override
    protected Potion createEntry() {
        return new Potion(getOwner().getModid() + ":" + getName(), effects);
    }
}
