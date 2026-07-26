package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.gl.GlComputePassBackend;
import org.lwjgl.opengl.ARBComputeShader;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.GL46;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
@org.jetbrains.annotations.ApiStatus.Internal
public class GlCommandEncoderMixin implements ALRCommandEncoderBackendExtension {
    @Shadow
    @Final
    private GlDevice device;

    @Override
    public void alrDispatchWorkgroups(int groupCountX, int groupCountY, int groupCountZ) {
        ARBComputeShader.glDispatchCompute(groupCountX, groupCountY, groupCountZ);
    }

    @Override
    public void alrDispatchWorkgroupsIndirect(GpuBufferSlice parameters) {
        GlStateManager._glBindBuffer(GL46.GL_DISPATCH_INDIRECT_BUFFER, ((GlBuffer) parameters.buffer()).handle);
        ARBComputeShader.glDispatchComputeIndirect(parameters.offset());
    }

    @Override
    public void alrMemoryBarrier(MemoryBarrierFlag... flags) {
        ARBShaderImageLoadStore.glMemoryBarrier(MemoryBarrierFlag.compound(flags));
    }

    @Override
    public ALRComputePass alrCreateComputePass() {
        return new ALRComputePass(new GlComputePassBackend(this, (ALRGpuDeviceBackendExtension) device));
    }
}
