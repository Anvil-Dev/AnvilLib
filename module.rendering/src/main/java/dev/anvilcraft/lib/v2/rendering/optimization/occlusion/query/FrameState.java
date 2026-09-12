package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.query;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.ALROptions;
import dev.anvilcraft.lib.v2.rendering.ALRPipelines;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query.GpuQueryObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.FullTransformsUbo;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.CullingStatistics;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionKey;
import it.unimi.dsi.fastutil.objects.Reference2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@ApiStatus.Internal
public class FrameState implements AutoCloseable {
    private final Map<OcclusionKey, GpuQueryObject> keySamplesMap = new IdentityHashMap<>();
    private final Reference2LongMap<OcclusionKey> results = new Reference2LongLinkedOpenHashMap<>();
    private final Reference2ObjectMap<Object, OcclusionKey> keyAssociations = new Reference2ObjectLinkedOpenHashMap<>();
    private final List<OcclusionKey> cameraInside = new ArrayList<>();
    private final List<OcclusionKey> frustumCulled = new ArrayList<>();
    private final GpuQueryOcclusionCuller owner;

    private int total = 0;
    private int culled = 0;
    private int features = 0;

    public FrameState(GpuQueryOcclusionCuller owner) {
        this.owner = owner;
    }

    public void addKey(OcclusionKey key, Object feature) {
        this.keyAssociations.put(feature, key);
        features++;
        GpuQueryObject gpuSamplesQuery = this.keySamplesMap.get(key);
        if (gpuSamplesQuery == null) {
            this.keySamplesMap.put(key, owner.acquireQuery());
            total++;
        }
    }

    public boolean shouldDraw(OcclusionKey key) {
        if (cameraInside.contains(key)) return true;
        if (ALROptions.OCCLUSION_QUERY_USE_FRUSTUM_PRE_PASS && frustumCulled.contains(key)) return false;
        return results.getOrDefault(key, 1) > 0;
    }

    public OcclusionKey getKey(Object feature) {
        return this.keyAssociations.get(feature);
    }

    public void fetchResults() {
        for (Map.Entry<OcclusionKey, GpuQueryObject> entry : keySamplesMap.entrySet()) {
            long value = entry.getValue().getValue();
            if (value <= 0) {
                this.culled++;
            }
            results.put(entry.getKey(), value);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void runQueries(CameraRenderState camera) {
        CommandEncoder commandEncoder = this.owner.getCommandEncoder();
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();

        RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer buffer = sequentialBuffer.getBuffer(6 * 6);
        VertexFormat.IndexType type = sequentialBuffer.type();

        GpuDevice device = RenderSystem.getDevice();

        ALRGpuDeviceExtension deviceExtension = (ALRGpuDeviceExtension) device;

        deviceExtension.alrPushDebugGroup(() -> "Gpu Occlusion Query Draw");

        List<QueryInstance> queries = new ArrayList<>();

        for (Map.Entry<OcclusionKey, GpuQueryObject> entry : keySamplesMap.entrySet()) {
            OcclusionKey key = entry.getKey();
            if (key.getBoundingBox().contains(camera.pos)) {
                cameraInside.add(key);
                continue;
            }
            if (ALROptions.OCCLUSION_QUERY_USE_FRUSTUM_PRE_PASS && !camera.cullFrustum.isVisible(key.getBoundingBox())) {
                this.frustumCulled.add(key);
            }
            QueryInstance query = this.owner.acquireInstance();
            query.prepareTransform(key, entry.getValue(), camera);
            queries.add(query);
        }

        FullTransformsUbo[] transforms = new FullTransformsUbo[queries.size()];
        for (int i = 0; i < queries.size(); i++) {
            if (!queries.get(i).isCameraInside()) {
                transforms[i] = queries.get(i).transformsUbo();
            }
        }

        DynamicUniformStorage<FullTransformsUbo> dynamicStorage = this.owner.getTransformsDynamicStorage();

        GpuBufferSlice[] gpuBufferSlices = dynamicStorage.writeUniforms(transforms);
        for (int i = 0; i < queries.size(); i++) {
            if (!queries.get(i).isCameraInside()) {
                queries.get(i).uniform(gpuBufferSlices[i]);
            }
        }

        try (RenderPass renderPass = commandEncoder.createRenderPass(
            () -> "Gpu Occlusion Query Draw Batch",
            target.getColorTextureView(),
            OptionalInt.empty(),
            target.getDepthTextureView(),
            OptionalDouble.empty()
        )) {
            renderPass.setPipeline(ALRPipelines.OCCLUSION_QUERY);

            for (QueryInstance query : queries) {
                if (query.isCameraInside()) continue;
                renderPass.setUniform("Transforms", query.uniform());
                renderPass.setVertexBuffer(0, query.vertexBuffer());
                renderPass.setIndexBuffer(buffer, type);

                query.queryObject().begin();
                renderPass.drawIndexed(0, 0, 6 * 6, 1);
                query.queryObject().end();
            }
        }

        for (QueryInstance query : queries) {
            this.owner.releaseInstance(query);
        }

        dynamicStorage.endFrame();

        deviceExtension.alrPopDebugGroup();
    }

    @Override
    public void close() {
        for (GpuQueryObject value : keySamplesMap.values()) {
            owner.releaseQuery(value);
        }
    }

    public boolean isEmpty() {
        return this.keyAssociations.isEmpty();
    }

    public CullingStatistics createStatistics() {
        int rendered = total - culled;
        return new CullingStatistics(
            total,
            this.frustumCulled.size(),
            this.cameraInside.size(),
            culled,
            rendered,
            features,
            null
        );
    }
}
