package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CommandEncoder.class)
@ApiStatus.Internal
public class CommandEncoderMixin implements ALRCommandEncoderExtension {
    @Shadow
    @Final
    private CommandEncoderBackend backend;

    @Override
    public void alrDispatchWorkgroups(int groupCountX, int groupCountY, int groupCountZ) {
        alrBackend().alrDispatchWorkgroups(groupCountX, groupCountY, groupCountZ);
    }

    @Override
    public void alrDispatchWorkgroupsIndirect(GpuBufferSlice parameters) {
        alrBackend().alrDispatchWorkgroupsIndirect(parameters);
    }

    private ALRCommandEncoderBackendExtension alrBackend() {
        return (ALRCommandEncoderBackendExtension) this.backend;
    }

    @Override
    public void alrMemoryBarrier(MemoryBarrierFlag... flags) {
        alrBackend().alrMemoryBarrier(flags);
    }

    @Override
    public ALRComputePass alrCreateComputePass() {
        return alrBackend().alrCreateComputePass();
    }
}
