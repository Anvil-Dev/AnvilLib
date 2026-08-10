package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader;

import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * A compiled GL compute program identified by its program id and the key it was compiled from.
 */
public record ALRComputeProgramInstance(int id, ALRComputeProgramInstanceKey key) {
    public static final ALRComputeProgramInstance INVALID = new ALRComputeProgramInstance(
        0,
        new ALRComputeProgramInstanceKey(
            AnvilLibRendering.location("compute/invalid"),
            Map.of()
        )
    );

}
