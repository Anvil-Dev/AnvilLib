package dev.anvilcraft.lib.v2.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionCuller;
import org.jspecify.annotations.Nullable;

public class ALROptimizations {
    private static OcclusionCuller occlusionCuller;

    public static OcclusionCuller getOcclusionCuller() {
        if (occlusionCuller == null) {
            occlusionCuller = OcclusionCuller.createInstance(RenderSystem.getDevice());
        }
        return occlusionCuller;
    }

    public static void create() {
        occlusionCuller = OcclusionCuller.createInstance(RenderSystem.getDevice());
    }
}
