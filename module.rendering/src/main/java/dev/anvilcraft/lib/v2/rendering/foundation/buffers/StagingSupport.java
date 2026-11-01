package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRHICapabilities;
import org.jetbrains.annotations.ApiStatus;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

@ApiStatus.Internal
public interface StagingSupport extends AutoCloseable {
    @Override
    default void close() {}


    ByteBuffer getBuffer(GpuDevice device, CommandEncoder commandEncoder, long size);

    void copyToBuffer(CommandEncoder commandEncoder, long offset, long size, GpuBufferSlice dest);

    static StagingSupport createInstance(GpuDevice device, String name) {
        ALRHICapabilities alrhiCapabilities = ALRHICapabilities.getInstance();
        if (device.getDeviceInfo().hintsAndWorkarounds().writeToBufferIsSlow()) {
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
        private @Nullable MappableRingBuffer stagingBuffers;
        private GpuBufferSlice.@Nullable MappedView mappedView;

        public GpuStagingSupport(String name) {
            this.name = name;
        }

        @Override
        public ByteBuffer getBuffer(GpuDevice device, CommandEncoder commandEncoder, long size) {
            if (stagingBuffers == null || stagingBuffers.size() < size) {
                close();
                stagingBuffers = new MappableRingBuffer(() -> name,
                    GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_MAP_WRITE, Math.toIntExact(size));
            }
            mappedView = stagingBuffers.currentBuffer().map(false, true);
            return mappedView.data();
        }

        @Override
        public void copyToBuffer(CommandEncoder commandEncoder, long offset, long size, GpuBufferSlice dest) {
            // Closing the view flushes non-coherent mapped writes before the GPU copies them.
            mappedView.close();
            mappedView = null;
            commandEncoder.copyToBuffer(stagingBuffers.currentBuffer().slice(offset, size), dest);
            stagingBuffers.rotate();
        }

        @Override
        public void close() {
            if (mappedView != null) {
                mappedView.close();
                mappedView = null;
            }
            if (stagingBuffers != null) {
                stagingBuffers.close();
                stagingBuffers = null;
            }
        }
    }
}
