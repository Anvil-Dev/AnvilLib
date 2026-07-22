package dev.anvilcraft.lib.v2.yukkuri.api.vapor;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Minimal large-cauldron contract required by the Yukkuri runtime. */
public interface VaporizationCauldron {
    BlockPos vaporizationPos();

    boolean isMainVaporizationPart();

    FluidStack getTopVaporizationFluid();

    /** Simulates or executes draining the exact fluid selected by a vaporization offer. */
    FluidStack drainVaporizationFluid(FluidStack resource, IFluidHandler.FluidAction action);
}
