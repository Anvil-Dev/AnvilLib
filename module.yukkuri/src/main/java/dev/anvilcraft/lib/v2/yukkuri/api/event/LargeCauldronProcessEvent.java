package dev.anvilcraft.lib.v2.yukkuri.api.event;

import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationContext;
import net.neoforged.bus.api.Event;

import java.util.Objects;

/** Fired around Yukkuri's vaporization transaction for one large cauldron. */
public final class LargeCauldronProcessEvent extends Event {
    private final VaporizationContext context;
    private final Phase phase;

    public LargeCauldronProcessEvent(VaporizationContext context, Phase phase) {
        this.context = Objects.requireNonNull(context, "context");
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    public VaporizationContext context() {
        return this.context;
    }

    public Phase phase() {
        return this.phase;
    }

    public enum Phase {
        BEFORE_VAPORIZATION,
        AFTER_VAPORIZATION
    }
}
