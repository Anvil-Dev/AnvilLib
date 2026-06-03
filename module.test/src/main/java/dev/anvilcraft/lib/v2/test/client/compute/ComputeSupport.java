package dev.anvilcraft.lib.v2.test.client.compute;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.GpuBufferConstants;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;

public class ComputeSupport {
    public static final ComputeSupport INSTANCE = new ComputeSupport();
    private final GpuDevice device = RenderSystem.getDevice();

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

    private final GpuBuffer addParamUBO = device.createBuffer(
        () -> "Test Compute Param UBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        AddParamUbo.LAYOUT.size(BufferLayout.STD140)
    );

    private final AddParamUbo addParam = new AddParamUbo();

    public float[] add(float[] input, float f) {
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
        }
        addParam.upload(commandEncoder, addParamUBO.slice());
        try (ALRComputePass pass = commandEncoderExtension.alrCreateComputePass()) {
            pass.setPipeline(TestPipelines.ADD);
            pass.bindAll(
                List.of(
                    addInputSSBO.slice(),
                    addOutputSSBO.slice(),
                    addParamUBO.slice()
                )
            );
            pass.dispatchWorkgroups((input.length + 16 - 1) / 16, 1, 1);
            pass.memoryBarrier(MemoryBarrierFlag.SHADER_STORAGE_BARRIER, MemoryBarrierFlag.BUFFER_UPDATE_BARRIER);
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
        return result;
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
