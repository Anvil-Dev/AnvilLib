package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.SoundEventEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

public class SoundEventBuilder<P> extends AbstractBuilder<SoundEvent, SoundEvent, P, SoundEventBuilder<P>>{
    @Nullable
    Float fix;
    public SoundEventBuilder(AbstractRegistrum<?> owner,
                             P parent,
                             String name,
                             BuilderCallback callback) {
        super(owner, parent, name, callback, Registries.SOUND_EVENT);
        this.fix = null;
    }

    public SoundEventBuilder<P> fix(float fix) {
        this.fix = fix;
        return this;
    }

    @Override
    public SoundEventEntry register() {
        return (SoundEventEntry) super.register();
    }

    @Override
    protected SoundEventEntry createEntryWrapper(DeferredHolder<SoundEvent, SoundEvent> delegate) {
        return new SoundEventEntry(getOwner(), delegate);
    }

    @Override
    protected SoundEvent createEntry() {
        Identifier location = Identifier.fromNamespaceAndPath(
                getOwner().getModid(),
                getName()
        );
        return fix == null ? SoundEvent.createVariableRangeEvent(location) :
                SoundEvent.createFixedRangeEvent(location, fix);
    }
}
