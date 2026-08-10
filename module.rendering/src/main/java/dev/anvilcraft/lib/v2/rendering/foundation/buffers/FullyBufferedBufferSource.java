package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link MultiBufferSource.BufferSource} that never draws: it accumulates one {@link BufferBuilder}
 * per {@link RenderType} and hands the finished {@link MeshData} to a {@link VertexBufferHost} through
 * {@link #upload(VertexBufferHost)}.
 * <p>
 * Ported from 26.1: the meshes used to be copied into {@code GpuBuffer}s through a
 * {@code CommandEncoder}; on 1.21.1 they are uploaded into {@link VertexBuffer}s on the render thread
 * (the upload runs via {@link VertexBufferHost#acceptUploadAction}, executed by the cached-BER pipeline
 * at {@code RenderFrameEvent.Pre}). The per-type {@code ByteBufferBuilder}s (786432 bytes each) must be
 * large enough for the whole compiled chunk layer.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FullyBufferedBufferSource extends MultiBufferSource.BufferSource implements AutoCloseable {
    private final Map<RenderType, ByteBufferBuilder> byteBuffers = new HashMap<>();
    private final Map<RenderType, BufferBuilder> bufferBuilders = new HashMap<>();
    @Getter
    private final Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();
    @Getter
    private final Map<RenderType, MeshData.SortState> meshSorts = new HashMap<>();

    public FullyBufferedBufferSource() {
        // The parent's shared/fixed buffers are never touched: getBuffer/endBatch are fully overridden.
        super(null, null);
    }

    private ByteBufferBuilder getByteBuffer(RenderType renderType) {
        return byteBuffers.computeIfAbsent(renderType, key -> new ByteBufferBuilder(786432));
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return bufferBuilders.computeIfAbsent(
            renderType,
            it -> new BufferBuilder(
                getByteBuffer(renderType),
                it.mode(),
                it.format()
            )
        );
    }

    /**
     * @return whether no render type was requested while compiling this batch. A render type whose
     * builder ended up with zero vertices is not reported here (it yields an empty mesh and is
     * skipped during upload), so an exact "no vertices anywhere" answer would require per-type
     * vertex counters that 1.21.1's {@link BufferBuilder} does not expose.
     */
    public boolean isEmpty() {
        return bufferBuilders.isEmpty();
    }

    @Override
    public void endBatch(RenderType renderType) {
    }

    @Override
    public void endLastBatch() {
    }

    @Override
    public void endBatch() {
    }

    public void upload(VertexBufferHost host) {
        for (RenderType renderType : bufferBuilders.keySet()) {
            host.acceptUploadAction(() -> uploadNow(host, renderType));
        }
    }

    @SuppressWarnings("resource")
    private void uploadNow(VertexBufferHost host, RenderType renderType) {
        BufferBuilder bufferBuilder = bufferBuilders.get(renderType);
        ByteBufferBuilder byteBuffer = byteBuffers.get(renderType);
        MeshData mesh = bufferBuilder.build();
        if (mesh != null) {
            indexCountMap.put(renderType, mesh.drawState().indexCount());
            if (renderType.sortOnUpload()) {
                MeshData.SortState sortState = mesh.sortQuads(
                    host.getSortingByteBufferBuilder(renderType),
                    RenderSystem.getVertexSorting()
                );
                meshSorts.put(renderType, sortState);
            }

            VertexBuffer vertexBuffer = host.getVertexBuffer(
                renderType,
                (long) mesh.drawState().vertexCount() * mesh.drawState().format().getVertexSize()
            );
            vertexBuffer.bind();
            vertexBuffer.upload(mesh);
        } else {
            indexCountMap.put(renderType, 0);
            meshSorts.remove(renderType);
        }
        byteBuffer.close();
        bufferBuilders.remove(renderType);
        byteBuffers.remove(renderType);
    }

    public void close(RenderType renderType) {
        ByteBufferBuilder builder = byteBuffers.get(renderType);
        builder.close();
    }

    public void close() {
        byteBuffers.keySet().forEach(this::close);
    }
}
