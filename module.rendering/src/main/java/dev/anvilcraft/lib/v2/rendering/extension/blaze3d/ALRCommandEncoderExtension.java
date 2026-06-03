package dev.anvilcraft.lib.v2.rendering.extension.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;

public interface ALRCommandEncoderExtension {

    void alrDispatchWorkgroups(
        int groupCountX,
        int groupCountY,
        int groupCountZ
    );

    void alrDispatchWorkgroupsIndirect(
        GpuBuffer parameters,
        long offset
    );

    void alrMemoryBarrier(MemoryBarrierFlag... flags);
}
