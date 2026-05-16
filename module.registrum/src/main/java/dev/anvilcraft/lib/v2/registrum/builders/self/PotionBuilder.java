package dev.anvilcraft.lib.v2.registrum.builders.self;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.builders.SelfBuilder;
import dev.anvilcraft.lib.v2.registrum.util.entry.PotionEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;

public class PotionBuilder<P> extends SelfBuilder<Potion, P, PotionBuilder<P>> {
    public PotionBuilder(AbstractRegistrum<?> owner,
                         P parent,
                         String name,
                         BuilderCallback callback,
                         MobEffectInstance... effects) {
        super(owner, parent, name, callback, Registries.POTION, () -> new Potion(owner.getModid() + ":" + name, effects));

    }

    @Override
    public PotionEntry register() {
        return (PotionEntry) super.register();
    }

    @Override
    protected PotionEntry createEntryWrapper(DeferredHolder<Potion, Potion> delegate) {
        return new PotionEntry(getOwner(), delegate);
    }
}
