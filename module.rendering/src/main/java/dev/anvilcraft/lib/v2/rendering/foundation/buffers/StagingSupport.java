package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRHICapabilities;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

@ApiStatus.Internal
public interface StagingSupport {

    ByteBuffer getBuffer(GpuDevice device, CommandEncoder commandEncoder, long size);

    void copyToBuffer(CommandEncoder commandEncoder, long offset, long size, GpuBufferSlice dest);

    static StagingSupport createInstance(GpuDevice device, String name) {
        GraphicsWorkarounds graphicsWorkarounds = GraphicsWorkarounds.get(device);
        ALRHICapabilities alrhiCapabilities = ALRHICapabilities.getInstance();
        if (graphicsWorkarounds.isGlOnDx12()) {
            return new CpuStagingSupport();
        }
        if (alrhiCapabilities.persistentMappedBuffer()) {
            return new GpuStagingSupport(name);
        }
        return new CpuStagingSupport();
    }

    class CpuStagingSupport implements StagingSupport {
        @Nullable
        private ByteBuffer buffer;
        private long size;

        @Override
        public ByteBuffer getBuffer(GpuDevice device, CommandEncoder commandEncoder, long size) {
            if (buffer == null || this.size < size) {
                buffer = ByteBuffer.allocateDirect((int) size);
                this.size = size;
            }
            return buffer;
        }

        @SuppressWarnings("DataFlowIssue")
        @Override
        public void copyToBuffer(CommandEncoder commandEncoder, long offset, long size, GpuBufferSlice dest) {
            commandEncoder.writeToBuffer(dest, buffer.slice((int) offset, (int) size));
        }
    }

    class GpuStagingSupport implements StagingSupport {
        private final String name;

        @Nullable
        private GpuBuffer stagingBuffer;
        private GpuBuffer.@Nullable MappedView mappedView;

        public GpuStagingSupport(String name) {
            this.name = name;
        }

        @Override
        @SuppressWarnings("DataFlowIssue")
        public ByteBuffer getBuffer(GpuDevice device, CommandEncoder commandEncoder, long size) {
            if (stagingBuffer == null || stagingBuffer.size() < size) {
                if (stagingBuffer != null) {
                    mappedView.close();
                    stagingBuffer.close();
                }
                this.stagingBuffer = device.createBuffer(
                    () -> name,
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_MAP_READ,
                    size
                );
                this.mappedView = commandEncoder.mapBuffer(stagingBuffer, true, true);
            }
            return this.mappedView.data();
        }

        @SuppressWarnings("DataFlowIssue")
        @Override
        public void copyToBuffer(CommandEncoder commandEncoder, long offset, long size, GpuBufferSlice dest) {
            commandEncoder.copyToBuffer(stagingBuffer.slice(offset, size), dest);
        }
    }
}
