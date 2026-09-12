package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.query;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query.GpuQueryObject;
import dev.anvilcraft.lib.v2.rendering.foundation.GpuReusableResource;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.FullTransformsUbo;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionKey;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
public final class QueryInstance implements GpuReusableResource {

    /// use single unit box and model view matrix instead
    /// ((3 float * 4) for each quad * 6) for cube * 1
    public static final int DEFAULT_VERTEX_BUFFER_SIZE = 3 * 4 * 4 * 6;

    /// Shared between QueryInstance instances
    private final GpuBuffer vertexBuffer;

    private final FullTransformsUbo transformsUbo;

    @Nullable
    private OcclusionKey key;

    @Nullable
    private GpuQueryObject queryObject;
    @Nullable
    private GpuBufferSlice uniform;

    private boolean closed = false;
    private boolean acquired = false;

    @Setter
    @Getter
    private boolean cameraInside = false;

    public QueryInstance(
        GpuBuffer vertexBuffer,
        FullTransformsUbo transformsUbo
    ) {
        this.vertexBuffer = vertexBuffer;
        this.transformsUbo = transformsUbo;
    }

    void prepareTransform(OcclusionKey key, GpuQueryObject queryObject, CameraRenderState camera) {
        this.key = key;
        this.queryObject = queryObject;

        this.transformsUbo.getProjMat().set(camera.projectionMatrix);

        Matrix4f cameraViewMat = this.transformsUbo.getCameraViewMat();
        cameraViewMat.set(camera.viewRotationMatrix);

        Matrix4f modelViewMat = this.transformsUbo.getModelViewMat();
        modelViewMat.identity();

        Vec3 cameraPos = camera.pos;
        modelViewMat.translate(
            (float) -cameraPos.x,
            (float) -cameraPos.y,
            (float) -cameraPos.z
        );

        AABB boundingBox = key.getBoundingBox().inflate(0.1);

        Vector3f min = new Vector3f(
            (float) boundingBox.minX,
            (float) boundingBox.minY,
            (float) boundingBox.minZ
        );

        Vector3f max = new Vector3f(
            (float) boundingBox.maxX,
            (float) boundingBox.maxY,
            (float) boundingBox.maxZ
        );

        // ChatGPT can make mistakes. Check important info.
        modelViewMat.translate(min)
            .scale(
                max.x - min.x,
                max.y - min.y,
                max.z - min.z
            );
    }

    @Override
    public void acquire() {
        this.acquired = true;
    }

    @Override
    public void release() {
        this.acquired = false;
        this.key = null;
        this.queryObject = null;
        this.uniform = null;
    }

    @Override
    public boolean isAcquired() {
        return this.acquired;
    }

    @Override
    public void close() {
        if (!closed) {
            // nothing to close if we handle all lifecycle logic correct
            this.closed = true;
        }
    }

    public GpuBuffer vertexBuffer() {
        return vertexBuffer;
    }

    public FullTransformsUbo transformsUbo() {
        return transformsUbo;
    }

    @Nullable
    public OcclusionKey key() {
        return key;
    }

    @Nullable
    public GpuQueryObject queryObject() {
        return queryObject;
    }

    @Nullable
    public GpuBufferSlice uniform() {
        return uniform;
    }

    public void uniform(GpuBufferSlice uniform) {
        this.uniform = uniform;
    }

    public static QueryInstance newInstance(CreationContext context, int index) {
        return new QueryInstance(
            context.getVertexBuffer(),
            new FullTransformsUbo()
        );
    }

    public record CreationContext(
        CommandEncoder commandEncoder,
        GpuDevice device
    ) {
        /// should keep alive when game running
        private static GpuBuffer vertexBuffer;

        @NonNull
        public GpuBuffer getVertexBuffer() {
            if (vertexBuffer == null) {
                prepareMesh(this);
            }
            return vertexBuffer;
        }

        private static void prepareMesh(CreationContext context) {
            float x0 = 0;
            float y0 = 0;
            float z0 = 0;
            float x1 = 1;
            float y1 = 1;
            float z1 = 1;

            BufferBuilder bufferBuilder = Tesselator.getInstance()
                .begin(
                    VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION
                );

            // z+
            bufferBuilder.addVertex(x0, y0, z1);
            bufferBuilder.addVertex(x1, y0, z1);
            bufferBuilder.addVertex(x1, y1, z1);
            bufferBuilder.addVertex(x0, y1, z1);

            // z-
            bufferBuilder.addVertex(x0, y0, z0);
            bufferBuilder.addVertex(x0, y1, z0);
            bufferBuilder.addVertex(x1, y1, z0);
            bufferBuilder.addVertex(x1, y0, z0);

            // x+
            bufferBuilder.addVertex(x1, y0, z0);
            bufferBuilder.addVertex(x1, y0, z1);
            bufferBuilder.addVertex(x1, y1, z1);
            bufferBuilder.addVertex(x1, y1, z0);

            // x-
            bufferBuilder.addVertex(x0, y0, z0);
            bufferBuilder.addVertex(x0, y1, z0);
            bufferBuilder.addVertex(x0, y1, z1);
            bufferBuilder.addVertex(x0, y0, z1);

            // y+
            bufferBuilder.addVertex(x0, y1, z0);
            bufferBuilder.addVertex(x0, y1, z1);
            bufferBuilder.addVertex(x1, y1, z1);
            bufferBuilder.addVertex(x1, y1, z0);

            // y-
            bufferBuilder.addVertex(x0, y0, z0);
            bufferBuilder.addVertex(x1, y0, z0);
            bufferBuilder.addVertex(x1, y0, z1);
            bufferBuilder.addVertex(x0, y0, z1);

            MeshData orThrow = bufferBuilder.buildOrThrow();

            if (vertexBuffer == null) {
                vertexBuffer = context.device.createBuffer(
                    () -> "Gpu Occlusion Query Vertex Buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    DEFAULT_VERTEX_BUFFER_SIZE
                );
            }

            context.commandEncoder.writeToBuffer(vertexBuffer.slice(), orThrow.vertexBuffer());

            orThrow.close();
        }

    }
}
