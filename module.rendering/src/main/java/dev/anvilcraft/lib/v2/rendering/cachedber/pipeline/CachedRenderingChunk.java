package dev.anvilcraft.lib.v2.rendering.cachedber.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import dev.anvilcraft.lib.v2.rendering.bloom.BloomPostEffect;
import dev.anvilcraft.lib.v2.rendering.extension.ALRRenderTypeExtension;
import dev.anvilcraft.lib.v2.rendering.foundation.ALRMeshSorting;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.VertexBufferHost;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A 16x16 block region of cached BER content: one {@link VertexBuffer} per {@link RenderType}, rebuilt
 * through {@link RebuildTask}s and drawn with frustum culling plus distance-based translucent sorting.
 * <p>
 * Ported from 26.1: {@code GpuBuffer}/{@code RenderPass} become {@link VertexBuffer} + the classic
 * {@code RenderSystem} draw path ({@code renderType.setupRenderState()} + {@code ShaderInstance} +
 * {@code VertexBuffer.bind()/draw()} — the same pattern {@code LevelRenderer.renderSectionLayer} uses).
 * The 26.1 per-type index {@code GpuBuffer}s are dropped: a 1.21.1 {@link VertexBuffer} owns its index
 * storage internally ({@code uploadIndexBuffer}), which also holds the sorted indices between frames.
 *
 * @author ZhuRuoLing
 */
public class CachedRenderingChunk implements VertexBufferHost {
    @Getter
    private final ChunkPos chunkPos;
    private final Map<RenderType, VertexBuffer> buffers = new HashMap<>();
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

    @Override
    public VertexBuffer getVertexBuffer(RenderType renderType, long size) {
        // 1.21.1's VertexBuffer has no size accessor and grows its GL storage on every upload,
        // so the 26.1 "reallocate when too small" check is dropped; reusing the instance is enough.
        return buffers.computeIfAbsent(renderType, key -> new VertexBuffer(VertexBuffer.Usage.STATIC));
    }

    @Override
    public ByteBufferBuilder getSortingByteBufferBuilder(RenderType renderType) {
        if (sortBuffers.containsKey(renderType)) {
            return sortBuffers.get(renderType);
        }
        ByteBufferBuilder builder = new ByteBufferBuilder(4096);
        sortBuffers.put(renderType, builder);
        return builder;
    }

    @Override
    public void acceptUploadAction(Runnable runnable) {
        this.pipeline.submitUploadTask(runnable);
    }

    private void renderInternal(Collection<RenderType> renderTypes, boolean translucent) {
        if (isEmpty) return;

        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;

        if (cameraPosition.distanceTo(new Vec3(
            chunkPos.x * 16,
            cameraPosition.y,
            chunkPos.z * 16
        )) > renderDistance) {
            return;
        }

        renderLayers(renderTypes, cameraPosition, translucent);
    }

    private void renderLayers(Collection<RenderType> renderingOrders, Vec3 cameraPosition, boolean translucent) {
        for (RenderType renderType : renderingOrders) {
            if (renderType.sortOnUpload() != translucent) continue;
            VertexBuffer vb = buffers.get(renderType);
            if (vb == null) continue;
            renderLayer(renderType, vb, cameraPosition);
            if (ALRRenderTypeExtension.isRenderingBloomed(renderType)) {
                BloomPostEffect bloomPostEffect = ALRPostEffects.getBloomPostEffect();
                if (bloomPostEffect != null) {
                    bloomPostEffect.beginBloomDraw();
                    renderLayer(renderType, vb, cameraPosition);
                    bloomPostEffect.endBloomDraw();
                }
            }
        }
    }

    public void releaseBuffers() {
        buffers.values().forEach(VertexBuffer::close);
        buffers.clear();
        sortBuffers.values().forEach(ByteBufferBuilder::close);
        sortBuffers.clear();
    }

    private void renderLayer(
        RenderType renderType,
        VertexBuffer vertexBuffer,
        Vec3 cameraPosition
    ) {
        MeshData.SortState sortState = this.meshSorting.get(renderType);
        int indexCount = indexCountMap.getInt(renderType);

        if (indexCount <= 0) return;

        if (sortState != null) {
            // Re-sort and re-upload the index buffer whenever the camera moved (or right after a
            // compile, since the compile-time sort used an arbitrary sorting origin).
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
                    vertexBuffer.bind();
                    // uploadIndexBuffer() takes ownership of (and closes) the result.
                    vertexBuffer.uploadIndexBuffer(result);
                    builder.clear();
                }
            }
        }

        renderType.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewStack.translate(
                -(float) cameraPosition.x + chunkPos.getMinBlockX(),
                -(float) cameraPosition.y,
                -(float) cameraPosition.z + chunkPos.getMinBlockZ()
            );
            RenderSystem.applyModelViewMatrix();

            shader.setDefaultUniforms(
                renderType.mode(),
                RenderSystem.getModelViewMatrix(),
                RenderSystem.getProjectionMatrix(),
                this.minecraft.getWindow()
            );
            shader.apply();
            vertexBuffer.bind();
            vertexBuffer.draw();
            shader.clear();
            VertexBuffer.unbind();

            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        renderType.clearRenderState();
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

    public void replaceMeshData(
        Map<RenderType, MeshData.SortState> meshSorts,
        Reference2IntMap<RenderType> indexCountMap
    ) {
        this.meshSorting = meshSorts;
        this.indexCountMap = indexCountMap;
        this.isFreshMesh = true;
    }
}
