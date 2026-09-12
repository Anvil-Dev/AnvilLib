package dev.anvilcraft.lib.v2.rendering.optimization.occlusion;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public record CullingStatistics(
    int total,
    int frustumPrePass,
    int cameraInside,
    int culled,
    int rendered,
    int features,
    List<String> message
) {

}
