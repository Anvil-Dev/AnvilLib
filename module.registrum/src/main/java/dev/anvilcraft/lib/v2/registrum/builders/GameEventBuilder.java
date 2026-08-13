package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.GameEventEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public class GameEventBuilder<P> extends AbstractBuilder<GameEvent, GameEvent, P, GameEventBuilder<P>> {
    final int radius;
    public GameEventBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, int radius) {
        super(owner, parent, name, callback, Registries.GAME_EVENT);
        this.radius = radius;
    }

    @Override
    public GameEventEntry register() {
        return (GameEventEntry) super.register();
    }

    @Override
    protected GameEventEntry createEntryWrapper(DeferredHolder<GameEvent, GameEvent> delegate) {
        return new GameEventEntry(getOwner(), delegate);
    }

    @Override
    protected GameEvent createEntry() {
        return new GameEvent(radius);
    }
}
