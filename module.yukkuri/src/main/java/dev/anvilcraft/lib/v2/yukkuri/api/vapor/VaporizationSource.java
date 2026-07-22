package dev.anvilcraft.lib.v2.yukkuri.api.vapor;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/** Discovers one vaporization method and creates exact, transactional processing offers. */
public interface VaporizationSource {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    /**
     * Creates an offer whose output is at most {@code maxVapor}. This method may be called more than once and must
     * not mutate world state. Returning {@code null} means this source cannot process the current top fluid.
     */
    @Nullable
    VaporizationOffer createOffer(VaporizationContext context, FluidStack availableInput, int maxVapor);

    /** Consumes source-specific resources and emits source-specific effects after the cauldron input is drained. */
    default void commit(VaporizationContext context, VaporizationOffer offer) {
    }
}
