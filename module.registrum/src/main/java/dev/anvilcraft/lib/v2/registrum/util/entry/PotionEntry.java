package dev.anvilcraft.lib.v2.registrum.util.entry;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;

public class PotionEntry extends SelfEntry<Potion> {
    public PotionEntry(AbstractRegistrum<?> owner, DeferredHolder<Potion, Potion> key) {
        super(owner, key);
    }

    public static PotionEntry cast(SelfEntry<Potion> entry) {
        return SelfEntry.cast(PotionEntry.class, entry);
    }
}
