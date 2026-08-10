package dev.anvilcraft.lib.v2.rendering.extension.blaze3d;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ALRCommandEncoderBackendExtension {
    void alrDispatchWorkgroups(
        int groupCountX,
        int groupCountY,
        int groupCountZ
    );

    void alrDispatchWorkgroupsIndirect(
        GpuBufferSlice parameters
    );

    void alrMemoryBarrier(MemoryBarrierFlag... flags);

    ALRComputePass alrCreateComputePass();
}
