package dev.anvilcraft.lib.v2.rendering.foundation.buffers.object;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public abstract class BufferObject<T extends BufferObject<T>> implements DynamicUniformStorage.DynamicUniform {

    protected final BufferLayout layout;
    protected final ShaderBufferObjectUsage usage;

    protected BufferObject(BufferLayout layout, ShaderBufferObjectUsage usage) {
        this.layout = layout;
        this.usage = usage;
    }

    protected abstract BufferObjectLayoutDefinition<T> getDefinition();

    @SuppressWarnings("unchecked")
    public void upload(CommandEncoder commandEncoder, GpuBufferSlice dest) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer malloc = getDefinition().write(memoryStack.malloc(getDefinition().size(this.layout)), (T) this, this.layout);
            commandEncoder.writeToBuffer(dest, malloc);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(@NonNull ByteBuffer buffer) {
        getDefinition().write(buffer, (T) this, this.layout);
    }

    public DynamicUniformStorage<T> createDynamicStorage(String label) {
        return new DynamicUniformStorage<>(
            label,
            getDefinition().size(this.layout),
            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST
        );
    }
}
