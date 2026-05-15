package dev.anvilcraft.lib.v2.rendering.blur;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.ALRPipelines;
import dev.anvilcraft.lib.v2.rendering.bloom.BlurParametersUbo;
import dev.anvilcraft.lib.v2.rendering.bloom.TransformsUbo;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class GaussianBlur implements AutoCloseable {
    private final RenderTarget inputTarget = new MainTarget(854, 480, false);
    private final RenderTarget tempTarget = new TextureTarget("BloomTemp", 854, 480, false);

    private final GpuDevice device = RenderSystem.getDevice();

    private final GpuBuffer blurUBO = device.createBuffer(
        () -> "BloomPostEffect->BlurUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        BlurParametersUbo.DEFINITION.size()
    );

    private final GpuBuffer vertexBuffer = device.createBuffer(
        () -> "BloomPostEffect->VertexBuffer",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX,
        1024
    );

    private final GpuBuffer transformUBO = device.createBuffer(
        () -> "BloomPostEffect->TransformUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        TransformsUbo.DEFINITION.size()
    );

    @Getter
    private final BlurParametersUbo blurParameters;

    private final TransformsUbo transformsUbo = new TransformsUbo(new Matrix4f());

    private final GpuSampler inputSampler = device.createSampler(
        AddressMode.CLAMP_TO_EDGE,
        AddressMode.CLAMP_TO_EDGE,
        FilterMode.LINEAR,
        FilterMode.LINEAR,
        1,
        OptionalDouble.empty()
    );

    private final GpuSampler mainSampler = device.createSampler(
        AddressMode.CLAMP_TO_EDGE,
        AddressMode.CLAMP_TO_EDGE,
        FilterMode.LINEAR,
        FilterMode.LINEAR,
        1,
        OptionalDouble.empty()
    );

    private int width;
    private int height;
    private int indexCount;

    public GaussianBlur(float sampleStepLength, float colorMultiplier) {
        Window window = Minecraft.getInstance().getWindow();

        this.width = window.getWidth();
        this.height = window.getHeight();
        this.blurParameters = new BlurParametersUbo(sampleStepLength, colorMultiplier, new Vector2f());

        resize(width, height);
    }

    public GpuTexture process(GpuTexture input) {
        CommandEncoder commandEncoder = device.createCommandEncoder();
        transformsUbo.upload(commandEncoder, transformUBO.slice());

        commandEncoder.clearColorTexture(tempTarget.getColorTexture(), 0);
        blurOnce(commandEncoder, input, tempTarget, true);
        commandEncoder.clearColorTexture(inputTarget.getColorTexture(), 0);
        blurOnce(commandEncoder, tempTarget.getColorTexture(), inputTarget, true);

        commandEncoder.clearColorTexture(tempTarget.getColorTexture(), 0);
        blurOnce(commandEncoder, inputTarget.getColorTexture(), tempTarget, false);
        commandEncoder.clearColorTexture(inputTarget.getColorTexture(), 0);
        blurOnce(commandEncoder, tempTarget.getColorTexture(), inputTarget, false);

        return inputTarget.getColorTexture();
    }

    @SuppressWarnings("DataFlowIssue")
    private void blurOnce(CommandEncoder commandEncoder, GpuTexture inputTarget, RenderTarget outputTarget, boolean horizontal) {
        Vector2f direction;
        if (horizontal) {
            direction = new Vector2f(1f, 0f);
        } else {
            direction = new Vector2f(0f, 1f);
        }
        blurParameters.setDirection(direction);
        blurParameters.upload(commandEncoder, blurUBO.slice());

        try (RenderPass blurPass = commandEncoder.createRenderPass(
            () -> "BloomPostEffect Blur " + (horizontal ? "H" : "V"),
            outputTarget.getColorTextureView(),
            OptionalInt.of(0)
        )) {
            blurPass.setPipeline(ALRPipelines.BLUR);
            blurPass.setUniform("Transforms", transformUBO);
            blurPass.setUniform("BlurParameters", blurUBO);
            blurPass.bindTexture("DiffuseSampler", device.createTextureView(inputTarget), inputSampler);
            blurPass.setVertexBuffer(0, vertexBuffer);
            RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            blurPass.setIndexBuffer(sequentialBuffer.getBuffer(indexCount), sequentialBuffer.type());
            blurPass.drawIndexed(0, 0, indexCount, 1);
        }
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        inputTarget.resize(width, height);
        tempTarget.resize(width, height);
        transformsUbo.getProjMat().setOrtho(
            0, width, 0, height, -1, -10000f
        );

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        builder.addVertex(0, 0, 100).setUv(0, 0);
        builder.addVertex(0, height, 100).setUv(0, 1);
        builder.addVertex(width, height, 100).setUv(1, 1);
        builder.addVertex(width, 0, 100).setUv(1, 0);

        MeshData data = builder.buildOrThrow();
        CommandEncoder commandEncoder = device.createCommandEncoder();
        commandEncoder.writeToBuffer(vertexBuffer.slice(), data.vertexBuffer());
        this.indexCount = data.drawState().indexCount();
        data.close();
    }

    @Override
    public void close() {
        blurUBO.close();
        vertexBuffer.close();
        transformUBO.close();
        inputSampler.close();
        mainSampler.close();
        inputTarget.destroyBuffers();
        tempTarget.destroyBuffers();
    }
}
