package dev.anvilcraft.lib.v2.yukkuri.api.vapor;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

/** Exact cauldron input and vapor output proposed by a source for one transaction. */
public record VaporizationOffer(FluidStack input, VaporStack output) {
    public VaporizationOffer {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        if (input.isEmpty() || input.getAmount() <= 0) {
            throw new IllegalArgumentException("Vaporization input must not be empty");
        }
        if (output.isEmpty()) throw new IllegalArgumentException("Vaporization output must not be empty");
        input = input.copy();
    }

    @Override
    public FluidStack input() {
        return this.input.copy();
    }
}
