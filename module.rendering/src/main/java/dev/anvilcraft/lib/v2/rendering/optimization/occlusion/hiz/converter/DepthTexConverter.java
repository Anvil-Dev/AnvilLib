package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.converter;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.ALRComputePipelines;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ExtendedTextureFormat;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import lombok.Getter;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.OptionalDouble;

/// Accepts a DEPTH_32F texture and convert it to R32F format texture with padding
public class DepthTexConverter {

    private final GpuDevice gpuDevice;
    private final ALRGpuDeviceExtension gpuDeviceExtension;

    private final ConvertDepthParamsUbo convertParams = new ConvertDepthParamsUbo();

    private final GpuBuffer convertParamsBuffer;
    private final GpuSampler convertSampler;

    private int framebufferWidth;
    private int framebufferHeight;

    private int paddedWidth;
    private int paddedHeight;

    private int dispatchDimensionX;
    private int dispatchDimensionY;

    @Getter
    private GpuTexture output;

    public DepthTexConverter(
        GpuDevice gpuDevice,
        ALRGpuDeviceExtension gpuDeviceExtension,
        RenderTarget mainRenderTarget,
        int paddedWidth,
        int paddedHeight
    ) {
        this.gpuDevice = gpuDevice;
        this.gpuDeviceExtension = gpuDeviceExtension;

        this.convertParamsBuffer = gpuDevice.createBuffer(
            () -> "SPD Depth Convert Params",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
            ConvertDepthParamsUbo.SIZE
        );

        this.convertSampler = gpuDevice.createSampler(
            AddressMode.CLAMP_TO_EDGE,
            AddressMode.CLAMP_TO_EDGE,
            FilterMode.LINEAR,
            FilterMode.LINEAR,
            1,
            OptionalDouble.empty()
        );

        this.onResize(mainRenderTarget.width, mainRenderTarget.height, paddedWidth, paddedHeight);
    }

    public void onResize(int width, int height, int paddedWidth, int paddedHeight) {
        this.framebufferWidth = width;
        this.framebufferHeight = height;

        this.paddedWidth = paddedWidth;
        this.paddedHeight = paddedHeight;

        this.dispatchDimensionX = Mth.ceil(paddedWidth / 32f);
        this.dispatchDimensionY = Mth.ceil(paddedHeight / 32f);

        this.depthConvertSetup(this.gpuDevice.createCommandEncoder());

        if (this.output != null) {
            this.output.close();
        }

        this.output = this.gpuDeviceExtension.alrCreateExtendedTexture(
            "HierarchicalZ Converted Depth Image",
            GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
            ExtendedTextureFormat.R32F,
            this.paddedWidth,
            this.paddedHeight,
            1,
            1
        );
    }

    public GpuTexture runConvert(CommandEncoder commandEncoder, GpuTexture source) {
        ALRCommandEncoderExtension commandEncoderExtension = (ALRCommandEncoderExtension) commandEncoder;

        try (ALRComputePass pass = commandEncoderExtension.alrCreateComputePass()) {
            pass.setPipeline(ALRComputePipelines.DEPTH_CONVERT);
            pass.bindUniformBlock(0, convertParamsBuffer.slice());
            pass.bindTexture(1, new TextureBinding.SamplerAndTexture(convertSampler, source));
            pass.bindImage(2, this.output, false, true);

            pass.dispatchWorkgroups(dispatchDimensionX, dispatchDimensionY, 1);
            pass.memoryBarrier(
                MemoryBarrierFlag.SHADER_IMAGE_ACCESS_BARRIER,
                MemoryBarrierFlag.TEXTURE_FETCH_BARRIER,
                MemoryBarrierFlag.TEXTURE_UPDATE_BARRIER
            );
        }

        return this.output;
    }

    private void depthConvertSetup(CommandEncoder commandEncoder) {
        this.convertParams.setSrcWidth(this.framebufferWidth);
        this.convertParams.setSrcHeight(this.framebufferHeight);
        this.convertParams.setPaddedWidth(this.paddedWidth);
        this.convertParams.setPaddedHeight(this.paddedHeight);
        this.convertParams.setPadValue(1);

        this.convertParams.upload(commandEncoder, this.convertParamsBuffer.slice());
    }
}
