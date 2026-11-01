package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jspecify.annotations.NullMarked;
import lombok.Getter;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ZhuRuoLing
 */
@ParametersAreNonnullByDefault
@NullMarked
@ApiStatus.Internal
public class FullyBufferedBufferSource implements AutoCloseable {
    private final Map<RenderType, ByteBufferBuilder> byteBuffers = new HashMap<>();
    private final Map<RenderType, BufferBuilder> bufferBuilders = new HashMap<>();
    @Getter
    private final Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();
    @Getter
    private final Map<RenderType, MeshData.SortState> meshSorts = new HashMap<>();


    private ByteBufferBuilder getByteBuffer(RenderType renderType) {
        return byteBuffers.computeIfAbsent(renderType, _ -> new ByteBufferBuilder(786432));
    }

    public VertexConsumer getBuffer(RenderType renderType) {
        return bufferBuilders.computeIfAbsent(
            renderType,
            it -> new BufferBuilder(
                getByteBuffer(renderType),
                it.primitiveTopology(),
                it.format()
            )
        );
    }

    public boolean isEmpty() {
        return bufferBuilders.values().stream().noneMatch(it -> it.vertices > 0);
    }

    public void endBatch(RenderType renderType) {
    }

    public void endLastBatch() {
    }

    public void endBatch() {
    }

    public void upload(VertexBufferHost host) {
        for (RenderType renderType : java.util.List.copyOf(bufferBuilders.keySet())) {
            host.acceptUploadAction(() -> uploadNow(host, renderType));
        }
    }

    @SuppressWarnings("resource")
    private void uploadNow(VertexBufferHost host, RenderType renderType) {
        BufferBuilder bufferBuilder = bufferBuilders.get(renderType);
        ByteBufferBuilder byteBuffer = byteBuffers.get(renderType);
        int compiledVertices = bufferBuilder.vertices * bufferBuilder.format.getVertexSize();
        if (compiledVertices >= 0) {
            MeshData mesh = bufferBuilder.build();
            indexCountMap.put(renderType, bufferBuilder.primitiveTopology.indexCount(bufferBuilder.vertices));
            if (mesh != null) {
                if (renderType.sortOnUpload()) {
                    MeshData.SortState sortState = mesh.sortQuads(
                        host.getSortingByteBufferBuilder(renderType),
                        ProjectionType.PERSPECTIVE.vertexSorting()
                    );

                    meshSorts.put(renderType, sortState);
                }

                GpuBuffer vertexBuffer = host.getVertexBuffer(renderType, compiledVertices);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(vertexBuffer.slice(), mesh.vertexBuffer());
                mesh.close();
            }
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