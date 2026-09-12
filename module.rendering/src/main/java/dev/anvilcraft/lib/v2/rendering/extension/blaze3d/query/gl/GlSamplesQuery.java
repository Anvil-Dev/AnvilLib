package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query.GpuQueryObject;
import dev.anvilcraft.lib.v2.rendering.util.MemoryAccess;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

@ApiStatus.Internal
public class GlSamplesQuery implements GpuQueryObject {

    private final int id;
    private long result = -1;
    private boolean closed = false;
    private boolean acquired = false;

    public GlSamplesQuery() {
        this.id = GL46.glGenQueries();
    }

    @Override
    public void begin() {
        GL46.glBeginQuery(GL46.GL_ANY_SAMPLES_PASSED, id);
    }

    @Override
    public void end() {
        GL46.glEndQuery(GL46.GL_ANY_SAMPLES_PASSED);
    }

    @Override
    public long getValue() {
        RenderSystem.assertOnRenderThread();
        if (this.closed) {
            throw new IllegalStateException("GlSamplesQuery is closed");
        } else {
            if (result == -1) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    long buffer = stack.nmalloc(4);
                    GL46.glGetQueryObjectuiv(id, GL46.GL_QUERY_RESULT, buffer);
                    this.result = MemoryAccess.getInt(buffer);
                }
            }
        }
        return this.result;
    }

    @Override
    public void close() {
        if (!closed) {
            GL46.glDeleteQueries(id);
        }
        this.closed = true;
    }

    @Override
    public void acquire() {
        this.acquired = true;
    }

    @Override
    public void release() {
        this.acquired = false;
        this.result = -1;
    }

    @Override
    public boolean isAcquired() {
        return this.acquired;
    }
}
