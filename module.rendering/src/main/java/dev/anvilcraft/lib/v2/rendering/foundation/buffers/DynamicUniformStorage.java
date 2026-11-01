package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryStack;

/** Uploads frame-local uniforms through Renderpearl's fence-managed transient allocator. */
public final class DynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> {
    private final int size;

    public DynamicUniformStorage(String label, int size, int initialCapacity) {
        this.size = size;
    }

    public GpuBufferSlice writeUniform(T value) {
        var device = RenderSystem.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var data = stack.malloc(this.size);
            value.write(data);
            data.position(0).limit(this.size);
            return device.createCommandEncoder().transientMemory().uploadGpu(
                data, device.getDeviceInfo().limits().minUniformOffsetAlignment(), GpuBuffer.USAGE_UNIFORM);
        }
    }

    /** Storage retirement is performed by Renderpearl when the frame's GPU fence completes. */
    public void endFrame() {
    }

    @FunctionalInterface
    public interface DynamicUniform {
        void write(ByteBuffer data);
    }

    @SafeVarargs
    public final GpuBufferSlice[] writeUniforms(T... values) {
        var device = RenderSystem.getDevice();
        var memory = device.createCommandEncoder().transientMemory();
        int alignment = device.getDeviceInfo().limits().minUniformOffsetAlignment();
        GpuBufferSlice[] slices = new GpuBufferSlice[values.length];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var data = stack.malloc(this.size);
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) continue;
                data.clear();
                values[i].write(data);
                data.position(0).limit(this.size);
                slices[i] = memory.uploadGpu(data, alignment, GpuBuffer.USAGE_UNIFORM);
            }
        }
        return slices;
    }
}
