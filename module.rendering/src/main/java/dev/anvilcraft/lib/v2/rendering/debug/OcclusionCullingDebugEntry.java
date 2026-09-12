package dev.anvilcraft.lib.v2.rendering.debug;

import dev.anvilcraft.lib.v2.rendering.ALROptimizations;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.CullingStatistics;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionCuller;
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class OcclusionCullingDebugEntry implements DebugScreenEntry {
    public static final Identifier LOCATION = AnvilLibRendering.location("occlusion_culling");

    @Override
    public void display(
        @NonNull DebugScreenDisplayer displayer,
        @Nullable Level serverOrClientLevel,
        @Nullable LevelChunk clientChunk,
        @Nullable LevelChunk serverChunk
    ) {
        OcclusionCuller occlusionCuller = ALROptimizations.getOcclusionCuller();
        CullingStatistics statistics = occlusionCuller.collectStatistics();

        if (statistics == null) {
            displayer.addToGroup(
                LOCATION,
                List.of(
                    "[AnvilLib] OcclusionCulling " + occlusionCuller.getClass().getSimpleName(),
                    "Debug info unavailable."
                )
            );
            return;
        }

        String e1 = "[AnvilLib] OcclusionCulling " + occlusionCuller.getClass().getSimpleName();
        if (statistics.message() != null) {
            e1 += ": " + statistics.message();
        }
        displayer.addToGroup(
            LOCATION,
            List.of(
                e1,
                "Total: " + statistics.total(),
                "FrustumPrePass: " + statistics.frustumPrePass(),
                "CameraInside: " + statistics.cameraInside(),
                "Culled: " + statistics.culled(),
                "Rendered: " + statistics.rendered(),
                "Features: " + statistics.features()
            )
        );
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }

    @Override
    public DebugEntryCategory category() {
        return DebugEntryCategory.SCREEN_TEXT;
    }
}
