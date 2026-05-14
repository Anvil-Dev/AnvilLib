package dev.anvilcraft.lib.v2.registrum.util.entry;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;

public class PotionEntry extends RegistryEntry<Potion, Potion> {
    public PotionEntry(AbstractRegistrum<?> owner, DeferredHolder<Potion, Potion> key) {
        super(owner, key);
    }

    public static PotionEntry cast(RegistryEntry<Potion, Potion> entry) {
        return RegistryEntry.cast(PotionEntry.class, entry);
    }
}
