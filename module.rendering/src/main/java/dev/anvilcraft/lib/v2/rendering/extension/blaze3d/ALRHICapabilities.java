package dev.anvilcraft.lib.v2.rendering.extension.blaze3d;

import com.mojang.blaze3d.systems.RenderSystem;

public record ALRHICapabilities(
    boolean compute,
    boolean bindlessTexturing,
    boolean persistentMappedBuffer,
    boolean shaderSubgroup,
    int maxImageUnit
) {

    public static ALRHICapabilities getInstance() {
        return ((ALRGpuDeviceExtension) RenderSystem.getDevice()).alrhiCreateCapabilities();
    }
}
