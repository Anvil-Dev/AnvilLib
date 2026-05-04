package dev.anvilcraft.lib.v2.rendering.foundation.ubo;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public abstract class UboObject<T extends UboObject<T>> implements DynamicUniformStorage.DynamicUniform {

    protected abstract UboLayoutDefinition<T> getDefinition();

    @SuppressWarnings("unchecked")
    public void upload(CommandEncoder commandEncoder, GpuBufferSlice dest) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer malloc = getDefinition().write(memoryStack.malloc(getDefinition().size()), (T) this);
            commandEncoder.writeToBuffer(dest, malloc);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(@NonNull ByteBuffer buffer) {
        getDefinition().write(buffer, (T) this);
    }

    public DynamicUniformStorage<T> createDynamicStorage(String label) {
        return new DynamicUniformStorage<>(
                label,
                getDefinition().size(),
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST
        );
    }
}
