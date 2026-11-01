package dev.anvilcraft.lib.v2.rendering.cachedber.pipeline;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.pipeline.IndexType;
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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.oit.OitStage;
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
    private final List<PreparedLayer> preparedLayers = new ArrayList<>();

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

    public void prepare(Frustum frustum, Vec3 cameraPosition) {
        this.preparedLayers.clear();
        if (this.isEmpty || !frustum.isVisible(this.renderingBB)) return;
        int renderDistance = this.minecraft.options.getEffectiveRenderDistance() * 16;
        if (cameraPosition.distanceTo(new Vec3(this.chunkPos.getMinBlockX(), cameraPosition.y,
            this.chunkPos.getMinBlockZ())) > renderDistance) return;
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.translate(-(float) cameraPosition.x + this.chunkPos.getMinBlockX(),
                -(float) cameraPosition.y, -(float) cameraPosition.z + this.chunkPos.getMinBlockZ());
            for (var entry : this.buffers.entrySet()) {
                RenderType type = entry.getKey();
                int count = this.indexCountMap.getInt(type);
                if (count <= 0) continue;
                var indices = this.getIndexBuffer(type, cameraPosition, this.meshSorting.get(type), count);
                this.preparedLayers.add(new PreparedLayer(type, this.modifyRenderTypeIfNeeded(type).prepare(), entry.getValue(), indices, count));
            }
            this.isFreshMesh = false;
        } finally {
            modelView.popMatrix();
        }
    }

    public void render(Frustum frustum, boolean translucent) {
        this.prepare(frustum, this.minecraft.gameRenderer.mainCamera().position());
        for (PreparedLayer layer : this.preparedLayers) RenderSystem.getSequentialBuffer(layer.type().primitiveTopology()).resizeToRequestedIndexCount();
        RenderTarget target = this.minecraft.gameRenderer.mainRenderTarget();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Cached block entities", target.getColorTextureView(), Optional.empty(),
            target.getDepthTextureView(), OptionalDouble.empty())) {
            this.render(translucent, pass, null);
        }
    }

    public void render(boolean translucent, RenderPass pass, @Nullable OitStage stage) {
        for (PreparedLayer layer : this.preparedLayers) {
            if (layer.type().sortOnUpload() != translucent) continue;
            if (stage == null) layer.prepared().drawFromBuffer(layer.geometry(), pass);
            else layer.prepared().drawFromBufferOit(layer.geometry(), stage, pass);
        }
    }

    public void enqueueBloom() {
        var bloom = ALRPostEffects.getBloomPostEffect();
        for (PreparedLayer layer : this.preparedLayers) {
            if (!ALRRenderTypeExtension.isRenderingBloomed(layer.type())) continue;
            bloom.drawBloomed((collector, pose) -> {
                RenderTarget target = bloom.getBloomInputTarget();
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Cached block entity bloom", target.getColorTextureView(), Optional.empty(),
                    target.getDepthTextureView(), OptionalDouble.empty())) {
                    layer.prepared().drawFromBuffer(layer.geometry(), pass);
                }
            });
        }
    }

    private record PreparedLayer(RenderType type, PreparedRenderType prepared, GpuBuffer vertices,
                                 IndexGenerationResult indices, int count) {
        StagedVertexBuffer.ExecuteInfo geometry() {
            var sequential = RenderSystem.getSequentialBuffer(this.type.primitiveTopology());
            return new StagedVertexBuffer.ExecuteInfo(this.vertices,
                this.indices.indices() == null ? sequential.getBuffer() : this.indices.indices(),
                this.indices.indexType() == null ? sequential.type() : this.indices.indexType(),
                0, 0, this.count, this.type.primitiveTopology());
        }
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

    public void releaseBuffers() {
        buffers.values().forEach(GpuBuffer::close);
        indexBuffers.values().forEach(GpuBuffer::close);
        sortBuffers.values().forEach(ByteBufferBuilder::close);
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
            autoIndices.requestIndexCount(indexCount);
            indices = null;
            indexType = null;
        } else {
            if (isFreshMesh || CachedBlockEntityRenderingPipeline.isCameraMoved()) {
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
                    autoIndices.requestIndexCount(indexCount);
                    indices = null;
                    indexType = null;
                }
            } else {
                if (indexBuffers.containsKey(renderType)) {
                    indices = getIndexBuffer(renderType, (long) sortState.indexType().bytes * indexCount);
                    indexType = sortState.indexType();
                } else {
                    RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.primitiveTopology());
                    autoIndices.requestIndexCount(indexCount);
                    indices = null;
                    indexType = null;
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

    private record IndexGenerationResult(@Nullable GpuBuffer indices, @Nullable IndexType indexType) {
    }
}