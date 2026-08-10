package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Key identifying a compute program: shader location plus preprocessor defines.
 * <p>
 * Ported from 26.1: the 26.1 {@code ShaderDefines} becomes a simple {@link Map}{@code <String, String>}.
 */
public record ALRComputeProgramInstanceKey(ResourceLocation location, Map<String, String> defines) {
}
