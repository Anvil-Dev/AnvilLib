package dev.anvilcraft.lib.v2.rendering.optimization.occlusion;

import dev.anvilcraft.lib.v2.rendering.ALROptimizations;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public class OcclusionFeatureRendererHelper {
    private OcclusionFeatureRendererHelper() {
    }

    public static <T> List<T> filterVisibleFeatures(List<T> features) {
        OcclusionCuller culler = ALROptimizations.getOcclusionCuller();
        if (culler == null || culler.isEmpty()) {
            return features;
        }

        List<T> visibleFeatures = new ArrayList<>(features.size());
        for (T feature : features) {
            if (culler.shouldDraw(feature)) {
                visibleFeatures.add(feature);
            }
        }
        return visibleFeatures;
    }
}
