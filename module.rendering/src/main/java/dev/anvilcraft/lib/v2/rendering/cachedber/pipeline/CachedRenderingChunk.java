package dev.anvilcraft.lib.v2.rendering.cachedber.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.IndexType;
import net.minecraft.client.renderer.StagedVertexBuffer;
import java.util.Optional;
import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import dev.anvilcraft.lib.v2.rendering.extension.ALRRenderTypeExtension;
import dev.anvilcraft.lib.v2.rendering.foundation.ALRMeshSorting;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.VertexBufferHost;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author ZhuRuoLing
 */
@ApiStatus.Internal
public class CachedRenderingChunk implements VertexBufferHost {
    @Getter
    private final ChunkPos chunkPos;
    private final Map<RenderType, GpuBuffer> buffers = new HashMap<>();
    private final Map<RenderType, GpuBuffer> indexBuffers = new HashMap<>();
    private final Map<RenderType, ByteBufferBuilder> sortBuffers = new HashMap<>();
    @Getter
    private final Set<BlockEntity> blockEntities = new HashSet<>();
    private final CachedBlockEntityRenderingPipeline pipeline;
    private final Minecraft minecraft = Minecraft.getInstance();
    private final AABB renderingBB;
    private Map<RenderType, MeshData.SortState> meshSorting = new HashMap<>();
    private Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();
    @Nullable
    @Setter(AccessLevel.PACKAGE)
    private RebuildTask lastRebuildTask;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private boolean isEmpty = true;
    private boolean isFreshMesh = true;

    public CachedRenderingChunk(ChunkPos chunkPos, CachedBlockEntityRenderingPipeline pipeline) {
        this.chunkPos = chunkPos;
        this.pipeline = pipeline;
        this.renderingBB = new AABB(
            this.chunkPos.getMinBlockX(),
            -65,
            this.chunkPos.getMinBlockZ(),
            this.chunkPos.getMaxBlockX(),
            321,
            this.chunkPos.getMaxBlockZ()
        );
    }

    /**
     * Updates the block entities collection and triggers a rebuild of the region.
     * <p>
     *
     * @param be The block entity to update.
     * @see CachedBlockEntityRenderingPipeline#update(BlockEntity)
     */
    public void update(BlockEntity be, boolean forced) {
        if (lastRebuildTask != null) {
            lastRebuildTask.cancel();
        }
        boolean shouldRecompile = blockEntities.removeIf(BlockEntity::isRemoved);
        if (be.isRemoved()) {
            shouldRecompile |= blockEntities.remove(be);
            if (shouldRecompile) {
                pipeline.submitCompileTask(new RebuildTask(this));
            }
            return;
        }
        shouldRecompile |= blockEntities.add(be);
        if (shouldRecompile || forced) {
            pipeline.submitCompileTask(new RebuildTask(this));
        }
    }

    /**
     * Handles the removal of a block entity from the system and initiates a cache rebuild.
     * <p>
     * When a block entity is removed, this method is called to update the internal state of the system.
     * It cancels any ongoing rebuild tasks, removes the specified block entity from the collection,
     * cleans up any other removed block entities, and then submits a new rebuild task to the pipeline.
     *
     * @param be The block entity that has been removed.
     * @see CachedBlockEntityRenderingPipeline#blockRemoved(BlockEntity)
     */
    public void blockRemoved(BlockEntity be) {
        if (lastRebuildTask != null) {
            lastRebuildTask.cancel();
        }
        boolean removedAny = blockEntities.removeIf(BlockEntity::isRemoved) || blockEntities.remove(be);
        if (removedAny) {
            pipeline.submitCompileTask(new RebuildTask(this));
        }
    }

    public void render(Frustum frustum, boolean translucent) {
        if (!frustum.isVisible(renderingBB)) return;
        renderInternal(buffers.keySet(), translucent);
    }

    public GpuBuffer getBuffer(Map<RenderType, GpuBuffer> buffers, RenderType renderType, long size, int usage) {
        if (buffers.containsKey(renderType)) {
            GpuBuffer buffer = buffers.get(renderType);
            if (buffer.size() < size) {
                buffer = RenderSystem.getDevice().createBuffer(
                    () -> pipeline.truncateName(renderType.toString()),
                    usage | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC,
                    size
                );
                GpuBuffer old = buffers.put(renderType, buffer);
                if (old != null) {
                    old.close();
                }
            }

            return buffers.get(renderType);
        }
        GpuBuffer vb = RenderSystem.getDevice().createBuffer(
            () -> pipeline.truncateName(renderType.toString()),
            usage | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC,
            size
        );
        buffers.put(renderType, vb);
        return vb;
    }

    public GpuBuffer getVertexBuffer(RenderType renderType, long size) {
        return getBuffer(this.buffers, renderType, size, GpuBuffer.USAGE_VERTEX);
    }

    public GpuBuffer getIndexBuffer(RenderType renderType, long size) {
        return getBuffer(this.indexBuffers, renderType, size, GpuBuffer.USAGE_INDEX);
    }

//    public GpuBuffer getBloomBuffers(RenderType renderType, long size) {
//        return getBuffer(bloomedBuffers, renderType, size);
//    }

    public ByteBufferBuilder getSortingByteBufferBuilder(RenderType renderType) {
        if (sortBuffers.containsKey(renderType)) {
            return sortBuffers.get(renderType);
        }
        ByteBufferBuilder builder = new ByteBufferBuilder(4096);
        sortBuffers.put(renderType, builder);
        return builder;
    }

    private void renderInternal(Collection<RenderType> renderTypes, boolean translucent) {
        if (isEmpty) return;

        Vec3 cameraPosition = minecraft.gameRenderer.mainCamera().position();
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;

        if (cameraPosition.distanceTo(new Vec3(
            chunkPos.x() * 16,
            cameraPosition.y,
            chunkPos.z() * 16
        )) > renderDistance) {
            return;
        }

        renderLayers(renderTypes, cameraPosition, translucent);
    }

    private void renderLayers(Collection<RenderType> renderingOrders, Vec3 cameraPosition, boolean translucent) {
        for (RenderType renderType : renderingOrders) {
            //noinspection DataFlowIssue
            renderType = modifyRenderTypeIfNeeded(renderType);
            if (renderType.sortOnUpload() != translucent) continue;
            GpuBuffer vb = buffers.get(renderType);
            if (vb == null) continue;
            renderLayer(renderType, vb, cameraPosition);
            if (ALRRenderTypeExtension.isRenderingBloomed(renderType)) {
                ALRPostEffects.getBloomPostEffect().beginBloomDraw();
                renderLayer(renderType, vb, cameraPosition);
                ALRPostEffects.getBloomPostEffect().endBloomDraw();
            }
        }
    }

    public void releaseBuffers() {
        buffers.values().forEach(GpuBuffer::close);
        indexBuffers.values().forEach(GpuBuffer::close);
        sortBuffers.values().forEach(ByteBufferBuilder::close);
    }

    private void renderLayer(
        RenderType renderType,
        GpuBuffer vertexBuffer,
        Vec3 cameraPosition
    ) {
        MeshData.SortState sortState = this.meshSorting.get(renderType);
        int indexCount = indexCountMap.getInt(renderType);

        if (indexCount <= 0) return;

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        try {
            modelViewStack.translate(
                -(float) cameraPosition.x + chunkPos.getMinBlockX(),
                -(float) cameraPosition.y,
                -(float) cameraPosition.z + chunkPos.getMinBlockZ());
            IndexGenerationResult indices = getIndexBuffer(renderType, cameraPosition, sortState, indexCount);
            RenderTarget target = ALRPostEffects.getBloomPostEffect().isDrawingBloom()
                ? ALRPostEffects.getBloomPostEffect().getBloomInputTarget() : minecraft.gameRenderer.mainRenderTarget();
            var prepared = renderType.prepare();
            var previousColor = RenderSystem.outputColorTextureOverride;
            var previousDepth = RenderSystem.outputDepthTextureOverride;
            try {
                RenderSystem.outputColorTextureOverride = target.getColorTextureView();
                RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
                prepared.drawFromBuffer(vertexBuffer, indices.indices(), indices.indexType(), 0, 0, indexCount);
            } finally {
                RenderSystem.outputColorTextureOverride = previousColor;
                RenderSystem.outputDepthTextureOverride = previousDepth;
            }
        } finally {
            modelViewStack.popMatrix();
        }
    }

    private CachedRenderingChunk.IndexGenerationResult getIndexBuffer(
        RenderType renderType,
        Vec3 cameraPosition,
        MeshData.@Nullable SortState sortState,
        int indexCount
    ) {
        IndexType indexType;
        GpuBuffer indices;
        if (sortState == null) {
            RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.primitiveTopology());
            indices = autoIndices.getBuffer(indexCount);
            indexType = autoIndices.type();
        } else {
            if (isFreshMesh || CachedBlockEntityRenderingPipeline.isCameraMoved()) {
                if (isFreshMesh) {
                    isFreshMesh = false;
                }
                Vector3f relativePos = cameraPosition.toVector3f().sub(
                    chunkPos.getMinBlockX(),
                    0,
                    chunkPos.getMinBlockZ()
                );

                ByteBufferBuilder builder = this.getSortingByteBufferBuilder(renderType);
                ByteBufferBuilder.Result result = sortState.buildSortedIndexBuffer(
                    builder,
                    ALRMeshSorting.byDistance(relativePos)
                );


                if (result != null) {
                    indices = getIndexBuffer(renderType, (long) sortState.indexType().bytes * indexCount);
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(indices.slice(), result.byteBuffer());
                    indexType = sortState.indexType();
                    result.close();
                    builder.clear();
                } else {
                    RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.primitiveTopology());
                    indices = autoIndices.getBuffer(indexCount);
                    indexType = autoIndices.type();
                }
            } else {
                if (indexBuffers.containsKey(renderType)) {
                    indices = getIndexBuffer(renderType, (long) sortState.indexType().bytes * indexCount);
                    indexType = sortState.indexType();
                } else {
                    RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.primitiveTopology());
                    indices = autoIndices.getBuffer(indexCount);
                    indexType = autoIndices.type();
                }

            }
        }
        return new IndexGenerationResult(indices, indexType);
    }

    public void forcedUpdate() {
        pipeline.submitCompileTask(new RebuildTask(this));
    }

    private RenderType modifyRenderTypeIfNeeded(RenderType rt) {
        return rt;
    }

    public <E extends BlockEntity> void addIfPossible(E blockEntity) {
        if (!blockEntities.contains(blockEntity)) {
            blockEntities.add(blockEntity);
            pipeline.submitCompileTask(new RebuildTask(this));
        }
    }

    @Override
    public void acceptUploadAction(Runnable runnable) {
        this.pipeline.submitUploadTask(runnable);
    }

    public void replaceMeshData(
        Map<RenderType, MeshData.SortState> meshSorts,
        Reference2IntMap<RenderType> indexCountMap
    ) {
        this.meshSorting = meshSorts;
        this.indexCountMap = indexCountMap;
        this.indexBuffers.forEach((_, buffers) -> buffers.close());
        this.indexBuffers.clear();
        this.isFreshMesh = true;
    }

    private record IndexGenerationResult(GpuBuffer indices, IndexType indexType) {
    }
}