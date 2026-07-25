package dev.anvilcraft.lib.v2.test.client.compute;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.ALRComputeCapabilities;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.GpuBufferConstants;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;

public class ComputeSupport {
    public static final ComputeSupport INSTANCE;
    public static final float[] UNSUPPORTED = {};
    @Getter
    private final GpuDevice device = RenderSystem.getDevice();

    static {
        ComputeSupport instance;
        if (ALRComputeCapabilities.isComputeSupported()) {
            instance = new ComputeSupport();
        } else {
            instance = null;
        }
        INSTANCE = instance;
    }

    private final GpuBuffer addInputSSBO = device.createBuffer(
        () -> "Test Compute Input SSBO",
        GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_COPY_DST | GpuBufferConstants.USAGE_SHADER_STORAGE,
        4 * 1024
    );

    private final GpuBuffer addOutputSSBO = device.createBuffer(
        () -> "Test Compute Output SSBO",
        GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ | GpuBufferConstants.USAGE_SHADER_STORAGE,
        4 * 1024
    );

    private final GpuBuffer addOutputCounter = device.createBuffer(
        () -> "Test Compute Output Atomic Counter",
        GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ | GpuBufferConstants.USAGE_ATOMIC_COUNTER,
        4
    );

    private final GpuBuffer addParamUBO = device.createBuffer(
        () -> "Test Compute Param UBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        AddParamUbo.LAYOUT.size(BufferLayout.STD140)
    );

    @Getter
    private final GpuSampler theSampler = device.createSampler(
        AddressMode.CLAMP_TO_EDGE,
        AddressMode.CLAMP_TO_EDGE,
        FilterMode.LINEAR,
        FilterMode.LINEAR,
        1,
        OptionalDouble.empty()
    );

    private GpuTexture outputTexture = device.createTexture(
        "Test Compute Output Texture",
        GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
        TextureFormat.RGBA8,
        854, 480,
        1,
        1
    );

    @Getter
    private GpuTextureView outputTextureView = device.createTextureView(outputTexture);

    private final GpuBuffer blurParamUBO = device.createBuffer(
        () -> "Test Compute Blur Param UBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        BlurParamUbo.LAYOUT.size(BufferLayout.STD140)
    );

    private final AddParamUbo addParam = new AddParamUbo();
    private final BlurParamUbo blurParam = new BlurParamUbo();

    public float[] add(float[] input, float f) {
        if (!ALRComputeCapabilities.isComputeSupported()) {
            return UNSUPPORTED;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        ALRCommandEncoderExtension commandEncoderExtension = ALRCommandEncoderExtension.of(commandEncoder);
        addParam.arraySize = input.length;
        addParam.f1 = f;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int inputSize = 4 * input.length;
            ByteBuffer inputBuffer = stack.malloc(inputSize);
            for (float v : input) {
                inputBuffer.putFloat(v);
            }
            inputBuffer.rewind();
            commandEncoder.writeToBuffer(addInputSSBO.slice(0, inputSize), inputBuffer);
            ByteBuffer counterBuffer = stack.malloc(4);
            counterBuffer.putInt(0);
            counterBuffer.rewind();
            commandEncoder.writeToBuffer(addOutputCounter.slice(), counterBuffer);
        }
        addParam.upload(commandEncoder, addParamUBO.slice());
        try (ALRComputePass pass = commandEncoderExtension.alrCreateComputePass()) {
            pass.setPipeline(TestPipelines.ADD);
            pass.bindAll(
                List.of(
                    addInputSSBO.slice(),
                    addOutputSSBO.slice(),
                    addParamUBO.slice(),
                    addOutputCounter.slice()
                )
            );
            pass.dispatchWorkgroups(Math.ceilDiv(input.length, 16), 1, 1);
            pass.memoryBarrier(
                MemoryBarrierFlag.SHADER_STORAGE_BARRIER,
                MemoryBarrierFlag.ATOMIC_COUNTER_BARRIER,
                MemoryBarrierFlag.BUFFER_UPDATE_BARRIER
            );
        }
        try (GpuFence fence = commandEncoder.createFence()) {
            fence.awaitCompletion(Long.MAX_VALUE);
        }
        float[] result = new float[input.length];
        GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(addOutputSSBO, true, false);
        ByteBuffer data = mappedView.data();
        for (int i = 0; i < input.length; i++) {
            result[i] = data.getFloat();
        }

        GpuBuffer.MappedView mappedCounterBuffer = commandEncoder.mapBuffer(addOutputCounter, true, false);
        ByteBuffer counterData = mappedCounterBuffer.data();

        int anInt = counterData.getInt();
        if (anInt != input.length) {
            System.out.printf("Compute counter does not match with input size: %d/%d%n", anInt, input.length);
        }
        return result;
    }

    public void resize(int width, int height) {
        outputTextureView.close();
        outputTexture.close();
        outputTexture = device.createTexture(
            "Test Compute Output Texture",
            GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
            TextureFormat.RGBA8,
            width, height,
            1,
            1
        );
        outputTextureView = device.createTextureView(outputTexture);
    }

    public GpuTextureView computeBlur() {
        GpuTexture colorTexture = Minecraft.getInstance().getMainRenderTarget().getColorTexture();
        int width = colorTexture.getWidth(0);
        int height = colorTexture.getHeight(0);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        blurParam.width = width;
        blurParam.height = height;
        ALRCommandEncoderExtension commandEncoderExtension = ALRCommandEncoderExtension.of(commandEncoder);
        blurParam.upload(commandEncoder, blurParamUBO.slice());
        commandEncoder.clearColorTexture(outputTexture, 0);

        try (ALRComputePass pass = commandEncoderExtension.alrCreateComputePass()) {
            pass.setPipeline(TestPipelines.BLUR);
            pass.bindAll(
                List.of(
                    blurParamUBO.slice(),
                    new TextureBinding.SamplerAndTexture(theSampler, colorTexture),
                    outputTexture
                )
            );

            pass.dispatchWorkgroups(Math.ceilDiv(width, 16), Math.ceilDiv(height, 16), 1);
            pass.memoryBarrier(
                MemoryBarrierFlag.TEXTURE_UPDATE_BARRIER,
                MemoryBarrierFlag.SHADER_IMAGE_ACCESS_BARRIER,
                MemoryBarrierFlag.BUFFER_UPDATE_BARRIER
            );
        }

        return outputTextureView;
    }

    private static class BlurParamUbo extends BufferObject<BlurParamUbo> {
        int width;
        int height;

        public static final BufferObjectLayoutDefinition<BlurParamUbo> LAYOUT = BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<BlurParamUbo>ofInt().forGetter(it -> it.width).build(),
            BufferObjectLayoutEntry.<BlurParamUbo>ofInt().forGetter(it -> it.height).build()
        );

        protected BlurParamUbo() {
            super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
        }


        @Override
        protected BufferObjectLayoutDefinition<BlurParamUbo> getDefinition() {
            return LAYOUT;
        }
    }


    private static class AddParamUbo extends BufferObject<AddParamUbo> {

        float f1 = 1;
        int arraySize = 0;

        public static final BufferObjectLayoutDefinition<AddParamUbo> LAYOUT = BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<AddParamUbo>ofFloat().forGetter(it -> it.f1).build(),
            BufferObjectLayoutEntry.<AddParamUbo>ofInt().forGetter(it -> it.arraySize).build()
        );

        protected AddParamUbo() {
            super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
        }

        @Override
        protected BufferObjectLayoutDefinition<AddParamUbo> getDefinition() {
            return LAYOUT;
        }
    }


    public static void init() {

    }
}
