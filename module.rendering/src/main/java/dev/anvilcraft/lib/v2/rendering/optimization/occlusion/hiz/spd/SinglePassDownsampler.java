package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.spd;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.ALRComputePipelines;
import dev.anvilcraft.lib.v2.rendering.ALROptions;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRHICapabilities;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ExtendedTextureFormat;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.GpuBufferConstants;
import dev.anvilcraft.lib.v2.rendering.util.MemoryAccess;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

/// ## FidelityFX Single Pass Downsampler 2.2
/// FidelityFX SPD downsamples the input texture with a user-defined 2x2 kernel using LDS for intermediate storage. Optionally, wave operations can be used to share data between threads as additional optimization.
///
/// A single thread group downsamples a 64x64 tile of the input texture to 1x1. Afterwards, a global atomic counter is incremented. The thread group will then check the global atomic - if it's equal to the number of tiles, it processes the last tile as before, computing the last 6 MIP levels of the input texture.
///
/// The last tile consists of the 1x1 output from all thread groups.
///
/// This way it is possible to reduce a 4096x4096 texture to 1x1 in a single dispatch call.
@ApiStatus.Internal
public class SinglePassDownsampler {
    private final Logger logger = LoggerFactory.getLogger("SinglePassDownsampler");

    /// uint * 6
    public static final int SPD_GLOBAL_ATOMIC_COUNTER_SIZE = 4 * 6;

    private final Minecraft minecraft;
    private final ALRGpuDeviceExtension gpuDeviceExtension;
    private final GpuDevice gpuDevice;

    private final SPDConstantBuffer spdParams = new SPDConstantBuffer();

    private final GpuBuffer spdParamsBuffer;
    private final GpuBuffer spdGlobalAtomicCounterBuffer;

    private final GpuSampler inputSampler;

    @Getter
    private final boolean useBindlessTexturing;
    private final boolean ldsWaveOperations;

    @Getter
    private int framebufferWidth;
    @Getter
    private int framebufferHeight;

    @Getter
    private int paddedWidth;
    @Getter
    private int paddedHeight;

    private int dispatchDimensionX;
    private int dispatchDimensionY;

    /// mip layer count, excluding input layer (mip 0)
    private int mipLayerCount = 0;
    /// mip layer textures, excluding input layer (mip 0)
    @Getter
    private GpuTexture[] mipTextures;
    /// mip layers, excluding input layer (mip 0)
    @Getter
    private Vector2i[] mipLayers;

    public SinglePassDownsampler(
        Minecraft minecraft,
        ALRGpuDeviceExtension gpuDeviceExtension,
        GpuDevice gpuDevice,
        RenderTarget mainRenderTarget
    ) {
        this.minecraft = minecraft;
        this.gpuDeviceExtension = gpuDeviceExtension;
        this.gpuDevice = gpuDevice;

        this.spdParamsBuffer = gpuDevice.createBuffer(
            () -> "SPD Constant Buffer",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
            SPDConstantBuffer.SIZE
        );

        this.spdGlobalAtomicCounterBuffer = gpuDevice.createBuffer(
            () -> "SPD Global Atomic Counter",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_MAP_WRITE | GpuBufferConstants.USAGE_SHADER_STORAGE,
            SPD_GLOBAL_ATOMIC_COUNTER_SIZE
        );

        this.inputSampler = gpuDevice.createSampler(
            AddressMode.CLAMP_TO_EDGE,
            AddressMode.CLAMP_TO_EDGE,
            FilterMode.LINEAR,
            FilterMode.LINEAR,
            1,
            OptionalDouble.empty()
        );

        ALRHICapabilities capabilities = ALRHICapabilities.getInstance();

        this.useBindlessTexturing = capabilities.bindlessTexturing() && capabilities.maxImageUnit() < 16;

        this.onResize(mainRenderTarget.width, mainRenderTarget.height);

        this.clearAtomicCounter();

        boolean shaderSubgroup = ALRHICapabilities.getInstance().shaderSubgroup();
        if (shaderSubgroup) {
            this.logger.info("Using GL_KHR_shader_subgroup_quad for reducing");
            ldsWaveOperations = ALROptions.SPD_OPTION_WAVE_INTEROP_LDS;
        } else {
            ldsWaveOperations = false;
        }

    }

    public void onResize(int width, int height) {
        this.framebufferWidth = width;
        this.framebufferHeight = height;

        this.paddedWidth = Math.ceilDiv(width, 64) * 64;
        this.paddedHeight = Math.ceilDiv(height, 64) * 64;

        this.mipLayerCount = Math.min(
            Mth.floor(
                Mth.log2(
                    Math.max(
                        this.paddedWidth,
                        this.paddedHeight
                    )
                )
            ),
            12
        );

        this.dispatchDimensionX = Mth.ceil(paddedWidth / 64f);
        this.dispatchDimensionY = Mth.ceil(paddedHeight / 64f);

        this.deleteTextures();

        this.mipTextures = new GpuTexture[mipLayerCount];
        this.mipLayers = new Vector2i[mipLayerCount];

        // generates mip 1 - mipLayerCount
        for (int i = 1; i <= mipLayerCount; i++) {
            int mipW = Math.max(1, this.paddedWidth >> i);
            int mipH = Math.max(1, this.paddedHeight >> i);

            Vector2i mipLayer = new Vector2i();
            mipLayer.x = mipW;
            mipLayer.y = mipH;

            GpuTexture texture = this.gpuDeviceExtension.alrCreateExtendedTexture(
                "HierarchicalZ Mip Chain Image #" + i,
                GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                ExtendedTextureFormat.R32F,
                mipW,
                mipH,
                1,
                1
            );

            this.mipTextures[i - 1] = texture;
            this.mipLayers[i - 1] = mipLayer;
        }

        CommandEncoder commandEncoder = gpuDevice.createCommandEncoder();
        this.ffxSpdSetup(commandEncoder);
    }

    /// Setup required constant values for SPD (CPU).
    private void ffxSpdSetup(CommandEncoder commandEncoder) {
        this.spdParams.setMips(this.mipLayerCount);
        this.spdParams.setNumWorkGroups(this.dispatchDimensionX * this.dispatchDimensionY);
        this.spdParams.setWorkGroupOffset(new Vector2f(0, 0));
        this.spdParams.setInvInputSize(new Vector2f(1.0f / this.paddedWidth, 1.0f / this.paddedHeight));

        this.spdParams.upload(commandEncoder, this.spdParamsBuffer.slice());
    }

    private void clearAtomicCounter() {
        CommandEncoder commandEncoder = gpuDevice.createCommandEncoder();

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer buffer = memoryStack.malloc(SPD_GLOBAL_ATOMIC_COUNTER_SIZE);
            MemoryAccess.memset(MemoryAccess.memAddress(buffer), SPD_GLOBAL_ATOMIC_COUNTER_SIZE, (byte) 0);
            commandEncoder.writeToBuffer(spdGlobalAtomicCounterBuffer.slice(), buffer);
        }
    }

    public void spdDispatch(CommandEncoder commandEncoder, GpuTexture source) {
        if (this.useBindlessTexturing) {
            this.spdDispatchBindless(commandEncoder, source);
            return;
        }
        this.spdDispatchImpl(commandEncoder, source);
    }

    public void spdDispatchImpl(CommandEncoder commandEncoder, GpuTexture source) {
        // TODO validate this
        ALRCommandEncoderExtension commandEncoderExtension = (ALRCommandEncoderExtension) commandEncoder;
        List<GpuTexture> textures = new ArrayList<>(13);
        GpuTexture midTex;
        textures.add(source);
        textures.addAll(Arrays.asList(mipTextures));
        if (textures.size() < 7) {
            midTex = source;
        } else {
            midTex = textures.get(6);
        }
        while (textures.size() < 13) {
            textures.add(source);
        }
        try (ALRComputePass pass = commandEncoderExtension.alrCreateComputePass()) {
            if (this.ldsWaveOperations) {
                pass.setPipeline(ALRComputePipelines.FFX_SPD_DOWNSAMPLE_PASS);
            } else {
                pass.setPipeline(ALRComputePipelines.FFX_SPD_DOWNSAMPLE_PASS_NO_LDS);
            }
            pass.bindUniformBlock(0, spdParamsBuffer.slice());
            pass.bindTexture(0, new TextureBinding.SamplerAndTexture(this.inputSampler, source));
            pass.bindShaderStorage(0, spdGlobalAtomicCounterBuffer.slice());
            pass.bindImage(1, midTex, true, true);
            pass.bindArrayOfTexture(2, textures, true, true);

            pass.dispatchWorkgroups(dispatchDimensionX, dispatchDimensionY, 1);

            pass.memoryBarrier(
                MemoryBarrierFlag.SHADER_IMAGE_ACCESS_BARRIER,
                MemoryBarrierFlag.BUFFER_UPDATE_BARRIER,
                MemoryBarrierFlag.SHADER_STORAGE_BARRIER
            );
        }
    }

    public void spdDispatchBindless(CommandEncoder commandEncoder, GpuTexture source) {
        ALRCommandEncoderExtension commandEncoderExtension = (ALRCommandEncoderExtension) commandEncoder;
        List<GpuTexture> textures = new ArrayList<>(13);
        GpuTexture midTex;
        textures.add(source);
        textures.addAll(Arrays.asList(mipTextures));
        if (textures.size() < 7) {
            midTex = source;
        } else {
            midTex = textures.get(6);
        }
        while (textures.size() < 13) {
            textures.add(source);
        }
        try (ALRComputePass pass = commandEncoderExtension.alrCreateComputePass()) {
            if (this.ldsWaveOperations) {
                pass.setPipeline(ALRComputePipelines.FFX_SPD_DOWNSAMPLE_PASS_BINDLESS);
            } else {
                pass.setPipeline(ALRComputePipelines.FFX_SPD_DOWNSAMPLE_PASS_BINDLESS_NO_LDS);
            }
            pass.bindUniformBlock(0, spdParamsBuffer.slice());
            pass.bindTexture(0, new TextureBinding.SamplerAndTexture(this.inputSampler, source));
            pass.bindShaderStorage(0, spdGlobalAtomicCounterBuffer.slice());
            pass.bindImage(1, midTex, true, true);
            pass.bindBindlessImageArray("rw_input_downsample_src_mips", textures);

            pass.dispatchWorkgroups(dispatchDimensionX, dispatchDimensionY, 1);

            pass.memoryBarrier(
                MemoryBarrierFlag.SHADER_IMAGE_ACCESS_BARRIER,
                MemoryBarrierFlag.BUFFER_UPDATE_BARRIER,
                MemoryBarrierFlag.SHADER_STORAGE_BARRIER
            );
        }
    }

    private void deleteTextures() {
        if (mipTextures != null) {
            for (GpuTexture mipTexture : mipTextures) {
                mipTexture.close();
            }
        }
    }
}
