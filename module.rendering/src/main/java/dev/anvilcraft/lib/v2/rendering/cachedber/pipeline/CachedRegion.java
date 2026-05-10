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
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.VertexBufferHost;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author ZhuRuoLing
 */
public class CachedRegion implements VertexBufferHost {
    @Getter
    private final ChunkPos chunkPos;
    private final Map<RenderType, GpuBuffer> buffers = new HashMap<>();
    private final Map<RenderType, ByteBufferBuilder> sortBuffers = new HashMap<>();
    @Getter
    private final Set<BlockEntity> blockEntities = new HashSet<>();
    private final CachedBlockEntityRenderingPipeline pipeline;
    private final Minecraft minecraft = Minecraft.getInstance();
    private Map<RenderType, MeshData.SortState> meshSorting = new HashMap<>();
    private Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();
    @Nullable
    @Setter(AccessLevel.PACKAGE)
    private RebuildTask lastRebuildTask;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private boolean isEmpty = true;

    public CachedRegion(ChunkPos chunkPos, CachedBlockEntityRenderingPipeline pipeline) {
        this.chunkPos = chunkPos;
        this.pipeline = pipeline;
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

    public void render() {
        renderInternal(buffers.keySet());
    }

    public GpuBuffer getBuffer(Map<RenderType, GpuBuffer> buffers, RenderType renderType, long size) {
        if (buffers.containsKey(renderType)) {
            GpuBuffer buffer = buffers.get(renderType);

            if (buffer.size() < size) {
                buffer = RenderSystem.getDevice().createBuffer(renderType::toString, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC, size);
                buffers.put(renderType, buffer);
            }

            return buffers.get(renderType);
        }
        GpuBuffer vb = RenderSystem.getDevice().createBuffer(renderType::toString, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC, size);
        buffers.put(renderType, vb);
        return vb;
    }

    public GpuBuffer getVertexBuffer(RenderType renderType, long size) {
        return getBuffer(this.buffers, renderType, size);
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

    private void renderInternal(Collection<RenderType> renderTypes) {
        if (isEmpty) return;

        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().position();
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;

        if (cameraPosition.distanceTo(new Vec3(chunkPos.x() * 16, cameraPosition.y, chunkPos.z() * 16)) > renderDistance) {
            return;
        }

        List<RenderType> renderingOrders = new ArrayList<>(renderTypes);
        renderingOrders.sort(Comparator.comparingInt(a -> (a.sortOnUpload() ? 1 : 0)));

        for (RenderType renderType : renderingOrders) {
            GpuBuffer vb = buffers.get(renderType);
            if (vb == null) continue;
            renderLayer(renderType, vb, cameraPosition);
        }
    }

    public void releaseBuffers() {
        buffers.values().forEach(GpuBuffer::close);
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
        Consumer<Matrix4fStack> modelViewModifier = renderType.state.layeringTransform.getModifier();

        modelViewStack.pushMatrix();

        if (modelViewModifier != null) {
            modelViewModifier.accept(modelViewStack);
        }

        modelViewStack.translate(
            -(float) cameraPosition.x + chunkPos.getMinBlockX(),
            -(float) cameraPosition.y,
            -(float) cameraPosition.z + chunkPos.getMinBlockZ()
        );

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), renderType.state.textureTransform.getMatrix());
        Map<String, RenderSetup.TextureAndSampler> textures = renderType.state.getTextures();

        GpuBuffer indices;
        VertexFormat.IndexType indexType;
        if (sortState == null) {
            RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.mode());
            indices = autoIndices.getBuffer(indexCount);
            indexType = autoIndices.type();
        } else {
            ByteBufferBuilder.Result result = sortState.buildSortedIndexBuffer(
                this.getSortingByteBufferBuilder(renderType),
                VertexSorting.byDistance(cameraPosition.toVector3f())
            );

            if (result != null) {
                indices = renderType.state.pipeline.getVertexFormat().uploadImmediateIndexBuffer(result.byteBuffer());
                indexType = sortState.indexType();
                result.close();
            } else {
                RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(renderType.mode());
                indices = autoIndices.getBuffer(indexCount);
                indexType = autoIndices.type();
            }
        }

        RenderTarget renderTarget = renderType.state.outputTarget.getRenderTarget();
        GpuTextureView colorTexture = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : renderTarget.getColorTextureView();
        GpuTextureView depthTexture = renderTarget.useDepth
            ? (RenderSystem.outputDepthTextureOverride != null
               ? RenderSystem.outputDepthTextureOverride
               : renderTarget.getDepthTextureView()
        ) : null;

        //noinspection DataFlowIssue
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Immediate draw for " + renderType, colorTexture, OptionalInt.empty(), depthTexture, OptionalDouble.empty())) {
            renderPass.setPipeline(renderType.state.pipeline);
            ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissorState.enabled()) {
                renderPass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
            }

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertexBuffer);

            for (Map.Entry<String, RenderSetup.TextureAndSampler> entry : textures.entrySet()) {
                renderPass.bindTexture(entry.getKey(), entry.getValue().textureView(), entry.getValue().sampler());
            }

            renderPass.setIndexBuffer(indices, indexType);
            renderPass.drawIndexed(0, 0, indexCount, 1);
        }

        modelViewStack.popMatrix();
    }

    public void forcedUpdate() {
        pipeline.submitCompileTask(new RebuildTask(this));
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

    public void replaceMeshData(Map<RenderType, MeshData.SortState> meshSorts, Reference2IntMap<RenderType> indexCountMap) {
        this.meshSorting = meshSorts;
        this.indexCountMap = indexCountMap;
    }
}