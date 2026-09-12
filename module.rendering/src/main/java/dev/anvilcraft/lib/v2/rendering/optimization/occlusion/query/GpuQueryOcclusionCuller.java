package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.query;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query.GpuQueryObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.FullTransformsUbo;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.CullingStatistics;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionCuller;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionKey;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import lombok.Getter;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// ### How does this work
/// In a single frame query happens in order described below.
/// - Frame begin
///     - Read back query result in previous frame
/// - Minecraft collects features to draw in current frame
///     - Culler collects OcclusionKey for features requested to cull
/// - Minecraft draws solid terrain
/// - Culler draws bounding box of each OcclusionKey and submits query about whether any samples passed in each draw event
/// - Especially, if camera is inside a bounding box, the corresponding feature will always being drawn
/// - Minecraft draws features
///     - Ask culler whether a feature should be drawn, answers are based on the previous frame's query results (one-frame latency)
/// - ...
/// - Frame ends
@ApiStatus.Internal
public class GpuQueryOcclusionCuller implements OcclusionCuller {
    @Getter
    private final CommandEncoder commandEncoder;
    private final GpuSampleQueryPool sampleQueryPool;
    private final QueryInstancePool queryInstancePool;
    private final ALRGpuDeviceExtension extension;
    @Getter
    private final DynamicUniformStorage<FullTransformsUbo> transformsDynamicStorage;

    private FrameState previousFrameState = null;
    private FrameState currentFrameState = null;

    public GpuQueryOcclusionCuller(ALRGpuDeviceExtension extension) {
        this.extension = extension;
        this.sampleQueryPool = new GpuSampleQueryPool(extension);
        GpuDevice gpuDevice = (GpuDevice) extension;

        this.commandEncoder = gpuDevice.createCommandEncoder();

        this.queryInstancePool = new QueryInstancePool(
            new QueryInstance.CreationContext(
                commandEncoder,
                gpuDevice
            )
        );

        this.transformsDynamicStorage = new DynamicUniformStorage<>(
            "Gpu Query Occlusion Transforms Dynamic Uniform",
            FullTransformsUbo.SIZE,
            512
        );
    }

    @Override
    public void onResize(int newWidth, int newHeight) {
        // Query-based culling has no size-dependent resources.
    }

    @Override
    public void beforeExtract() {

    }

    @Override
    public void beginRenderingFrame() {
        if (this.currentFrameState == null) {
            this.currentFrameState = new FrameState(this);
            return;
        }
        if (this.previousFrameState != null) {
            this.previousFrameState.close();
        }
        this.previousFrameState = this.currentFrameState;
        this.previousFrameState.fetchResults();
        this.currentFrameState = new FrameState(this);
    }

    @Override
    public void submitFeatureKey(OcclusionKey key, List<Object> feature) {
        for (Object o : feature) {
            this.currentFrameState.addKey(key, o);
        }

    }

    @Override
    public void processFeatures(CameraRenderState camera) {
        this.currentFrameState.runQueries(camera);
    }

    @Override
    public boolean shouldDraw(Object feature) {
        if (previousFrameState == null) {
            return true;
        }
        OcclusionKey key = this.currentFrameState.getKey(feature);
        return this.previousFrameState.shouldDraw(key);
    }

    @Override
    public boolean isEmpty() {
        if (this.previousFrameState == null) {
            return true;
        }
        return this.previousFrameState.isEmpty();
    }

    @Override
    public @Nullable CullingStatistics collectStatistics() {
        if (this.previousFrameState == null) {
            return null;
        }
        return this.previousFrameState.createStatistics();
    }

    public GpuQueryObject acquireQuery() {
        return this.sampleQueryPool.acquire();
    }

    public void releaseQuery(GpuQueryObject query) {
        this.sampleQueryPool.release(query);
    }

    public QueryInstance acquireInstance() {
        return this.queryInstancePool.acquire();
    }

    public void releaseInstance(QueryInstance queryInstance) {
        this.queryInstancePool.release(queryInstance);
    }

    @Override
    public void close() throws Exception {

    }
}
