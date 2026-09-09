package dev.anvilcraft.lib.v2.rendering.optimization.occlusion;

import com.mojang.blaze3d.systems.GpuDevice;
import dev.anvilcraft.lib.v2.rendering.ALROptions;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.noop.NoOpOcclusionCuller;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/// Coordinates occlusion testing for render features across a frame.
///
/// A feature is recorded through an {@link OcclusionSubmitNodeStorage}, using a
/// stable {@link OcclusionKey} instance. The key must be retained by the feature
/// and reused on every frame; its bounding box may be updated in place.
///
/// A typical block-entity renderer submits its geometry as follows:
///
/// ```java
/// OcclusionCuller culler = ALROptimizations.getOcclusionCuller();
/// OcclusionSubmitNodeStorage storage = culler.wrapSubmitNodeStorage(collector);
/// storage.beginOcclusionRecord(state.occlusionKey);
/// renderGeometry(poseStack, storage);
/// storage.endOcclusionRecord();
/// ```
///
/// The rendering pipeline calls {@link #beforeExtract()}, {@link #beginRenderingFrame()},
/// and {@link #processFeatures(CameraRenderState)} at their respective frame
/// stages. Call {@link #shouldDraw(Object)} while extracting or submitting a
/// feature to skip geometry known to be occluded.
///
/// Always obtain the storage passed to a renderer through
/// {@link #wrapSubmitNodeStorage(SubmitNodeCollector)}. The other methods on
/// {@link OcclusionSubmitNodeStorage} are part of the internal recording
/// protocol and should not be called directly by library users.
public interface OcclusionCuller extends AutoCloseable{

    /// Updates the culler to use the new main-target dimensions.
    ///
    /// @param newWidth new target width in pixels
    /// @param newHeight new target height in pixels
    void onResize(int newWidth, int newHeight);

    /// Resets per-frame extraction state before any render features are extracted.
    void beforeExtract();

    /// Starts a new rendering frame and prepares the previous frame's results
    /// for visibility queries.
    void beginRenderingFrame();

    /// Associates submitted render objects with an occlusion key for this frame.
    ///
    /// @param key stable identity identifying the logical feature
    /// @param feature render objects recorded for the feature
    void submitFeatureKey(OcclusionKey key, List<Object> feature);

    /// Processes submitted features after solid and cutout terrain have rendered.
    ///
    /// @param camera camera state for the current level render
    void processFeatures(CameraRenderState camera);

    /// Returns whether a submitted render object should be drawn this frame.
    ///
    /// @param feature render object previously submitted with a key
    /// @return `true` when the object is visible or cannot be culled
    boolean shouldDraw(Object feature);

    /// Returns whether no features are currently tracked by the culler.
    ///
    /// @return `true` when the culler has no tracked features
    boolean isEmpty();

    /// Wraps a collector so feature geometry can be recorded against an
    /// {@link OcclusionKey} with `beginOcclusionRecord` and `endOcclusionRecord`.
    ///
    /// @param original collector used for the actual render submission
    /// @return storage that records occlusion information while delegating draws
    default OcclusionSubmitNodeStorage wrapSubmitNodeStorage(SubmitNodeCollector original) {
        return new OcclusionSubmitNodeStorage(this, original);
    }

    /// Collects implementation-specific visibility counters for debugging.
    ///
    /// @return current statistics, or `null` when statistics are unavailable
    @Nullable
    CullingStatistics collectStatistics();

    /// Creates the best supported culling implementation for the device.
    /// A forced implementation from {@link ALROptions#OCCLUSION_CULLING_FORCE_IMPL}
    /// is tried first, followed by hierarchical-Z, GPU queries, and finally a
    /// no-op implementation.
    ///
    /// @param device GPU device used to create the culler
    /// @return a usable culler; never `null`
    @SuppressWarnings("ConstantValue")
    static OcclusionCuller createInstance(GpuDevice device) {
        OcclusionCuller instance;

        String forcedImpl = ALROptions.OCCLUSION_CULLING_FORCE_IMPL;
        if (forcedImpl != null) {
            try {
                instance = OcclusionMethod.valueOf(forcedImpl).createInstance(device);
            } catch (IllegalArgumentException _) {
                instance = null;
            }
            if (instance != null) {
                return instance;
            }
        }
        if (OcclusionMethod.HIERARCHICAL_Z.isSupported()
            && (instance = OcclusionMethod.HIERARCHICAL_Z.createInstance(device)) != null
        ) {
            return instance;
        }

        if (OcclusionMethod.GPU_QUERY.isSupported()
            && (instance = OcclusionMethod.GPU_QUERY.createInstance(device)) != null
        ) {
            return instance;
        }

        return new NoOpOcclusionCuller();
    }
}
