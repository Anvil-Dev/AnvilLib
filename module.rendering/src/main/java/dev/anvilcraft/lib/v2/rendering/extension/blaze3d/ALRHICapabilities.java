package dev.anvilcraft.lib.v2.rendering.extension.blaze3d;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

@ApiStatus.Internal
public record ALRHICapabilities(
    boolean compute,
    boolean bindlessTexturing,
    boolean persistentMappedBuffer,
    boolean shaderSubgroup,
    int maxImageUnit,
    int[] maxComputeWorkgroup
) {

    public static ALRHICapabilities getInstance() {
        return ((ALRGpuDeviceExtension) RenderSystem.getDevice()).alrhiCreateCapabilities();
    }

    @Override
    @NonNull
    public String toString() {
        return "ALRHICapabilities{" +
            "compute=" + compute +
            ", bindlessTexturing=" + bindlessTexturing +
            ", persistentMappedBuffer=" + persistentMappedBuffer +
            ", shaderSubgroup=" + shaderSubgroup +
            ", maxImageUnit=" + maxImageUnit +
            ", maxComputeWorkgroup=" + Arrays.toString(maxComputeWorkgroup) +
            '}';
    }
}
