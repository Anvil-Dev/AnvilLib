package dev.anvilcraft.lib.v2.rendering.glitch;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.ALRPipelines;
import dev.anvilcraft.lib.v2.rendering.bloom.TransformsUbo;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import lombok.Getter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class GlitchPostEffect {
    public static final int UNIFORM_GLITCH_SIZE = GlitchParametersUbo.DEFINITION.size(BufferLayout.STD140);
    public static final int UNIFORM_TRANSFORM_SIZE = TransformsUbo.DEFINITION.size(BufferLayout.STD140);

    @Getter
    private final RenderTarget glitchOutputTarget = new TextureTarget("Glitch Result", 854, 480, false, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);

    private final GpuDevice device = RenderSystem.getDevice();

    private final GpuBuffer transformUBO = device.createBuffer(
        () -> "GlitchPostEffect TransformUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        UNIFORM_TRANSFORM_SIZE
    );

    private final GpuBuffer glitchParameterUBO = device.createBuffer(
        () -> "GlitchPostEffect TransformUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        UNIFORM_GLITCH_SIZE
    );

    private final GpuBuffer vertexBuffer = device.createBuffer(
        () -> "GlitchPostEffect VertexBuffer",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX,
        1024
    );

    @Getter
    private final GpuSampler sampler = device.createSampler(
        AddressMode.CLAMP_TO_EDGE,
        AddressMode.CLAMP_TO_EDGE,
        FilterMode.LINEAR,
        FilterMode.LINEAR,
        1,
        OptionalDouble.empty()
    );

    private final TransformsUbo transform = new TransformsUbo(new Matrix4f());
    private final GlitchParametersUbo glitchParameters = new GlitchParametersUbo();
    private int lastWidth = 0;
    private int lastHeight = 0;
    private int indexCount = 0;

    public GpuTextureView process(GpuTextureView input, int width, int height) {
        CommandEncoder commandEncoder = device.createCommandEncoder();

        if (glitchOutputTarget.width < width || glitchOutputTarget.height < height) {
            glitchOutputTarget.resize(width, height);
        }

        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level.getGameTime();
        DeltaTracker deltaTracker = minecraft.getDeltaTracker();

        if (lastHeight != height || lastWidth != width) {
            this.lastWidth = width;
            this.lastHeight = height;
            ByteBufferBuilder meshBuffer = new ByteBufferBuilder(1024);
            BufferBuilder builder = new BufferBuilder(meshBuffer, com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);

            builder.addVertex(0, 0, 100).setUv(0, 1);
            builder.addVertex(0, height, 100).setUv(0, 0);
            builder.addVertex(width, height, 100).setUv(1, 0);
            builder.addVertex(width, 0, 100).setUv(1, 1);

            MeshData data = builder.buildOrThrow();
            commandEncoder.writeToBuffer(vertexBuffer.slice(), data.vertexBuffer());
            this.indexCount = data.drawState().indexCount();
            data.close();
        meshBuffer.close();
        }

        transform.getProjMat().setOrtho(
            0,
            glitchOutputTarget.width,
            0,
            glitchOutputTarget.height,
            -10000,
            10000,
            device.getDeviceInfo().isZZeroToOne()
        );
        glitchParameters.getInSize().set(width, height);
        float uGameTime = (float) (gameTime % 24000L) + deltaTracker.getGameTimeDeltaPartialTick(false);
        uGameTime /= 10f;
        glitchParameters.setGameTime(uGameTime);

        transform.upload(commandEncoder, transformUBO.slice());
        glitchParameters.upload(commandEncoder, glitchParameterUBO.slice());
        RenderSystem.AutoStorageIndexBuffer buffer = RenderSystem.getSequentialBuffer(com.mojang.blaze3d.PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = buffer.getBuffer(indexCount);
        try (RenderPass pass = commandEncoder.createRenderPass(
            () -> "Glitch Post Pass",
            glitchOutputTarget.getColorTextureView(),
            java.util.Optional.of(new org.joml.Vector4f())
        )) {
            pass.setPipeline(ALRPipelines.GLITCH);
            pass.setUniform("Transforms", transformUBO);
            pass.setUniform("GlitchParameters", glitchParameterUBO);
            pass.bindTexture("DiffuseSampler", input, sampler);
            pass.setVertexBuffer(0, vertexBuffer.slice());
            pass.setIndexBuffer(indexBuffer, buffer.type());
            pass.drawIndexed(indexCount, 1, 0, 0, 0);
        }
        return glitchOutputTarget.getColorTextureView();
    }
}
