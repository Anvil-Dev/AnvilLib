package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.noop;

import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.CullingStatistics;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionCuller;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionKey;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ApiStatus.Internal
public class NoOpOcclusionCuller implements OcclusionCuller {
    @Override
    public void onResize(int newWidth, int newHeight) {

    }

    @Override
    public void beforeExtract() {

    }

    @Override
    public void beginRenderingFrame() {

    }

    @Override
    public void submitFeatureKey(OcclusionKey key, List<Object> feature) {

    }

    @Override
    public void processFeatures(CameraRenderState camera) {

    }

    @Override
    public boolean shouldDraw(Object feature) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public @Nullable CullingStatistics collectStatistics() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
