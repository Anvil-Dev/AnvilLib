package dev.anvilcraft.lib.v2.rendering.extension.blaze3d;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;

public interface ALRCommandEncoderExtension {

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

    static ALRCommandEncoderExtension of(CommandEncoder commandEncoder){
        return (ALRCommandEncoderExtension) commandEncoder;
    }
}
