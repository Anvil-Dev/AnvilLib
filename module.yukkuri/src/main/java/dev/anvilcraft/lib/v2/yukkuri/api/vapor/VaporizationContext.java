package dev.anvilcraft.lib.v2.yukkuri.api.vapor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

/** Stable positions and state shared by a large-cauldron vaporization transaction. */
public record VaporizationContext(ServerLevel level, VaporizationCauldron cauldron) {
    public VaporizationContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(cauldron, "cauldron");
    }

    public BlockPos cauldronPos() {
        return this.cauldron.vaporizationPos();
    }

    /** The first block directly above the large cauldron's top-center part. */
    public BlockPos outletPos() {
        return this.cauldronPos().above(2);
    }

    public FluidStack topFluid() {
        return this.cauldron.getTopVaporizationFluid().copy();
    }
}
