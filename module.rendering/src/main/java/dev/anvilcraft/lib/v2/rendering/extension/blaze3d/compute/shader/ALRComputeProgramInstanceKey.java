package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader;

import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

public record ALRComputeProgramInstanceKey(Identifier location, ShaderDefines defines) {
}
