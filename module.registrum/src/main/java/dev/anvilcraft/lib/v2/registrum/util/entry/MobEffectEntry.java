package dev.anvilcraft.lib.v2.registrum.util.entry;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MobEffectEntry<T extends MobEffect> extends RegistryEntry<MobEffect, T> {
    public MobEffectEntry(AbstractRegistrum<?> owner, DeferredHolder<MobEffect, T> key) {
        super(owner, key);
    }

    public static <T extends MobEffect> MobEffectEntry<T> cast(RegistryEntry<MobEffect, T> entry) {
        return RegistryEntry.cast(MobEffectEntry.class, entry);
    }
}
