package dev.anvilcraft.lib.v2.rendering.bloom;

import java.util.Optional;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import dev.anvilcraft.lib.v2.rendering.foundation.FeatureRendering;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.ALRPipelines;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.compound.CompoundSubmitNodeStorage;
import dev.anvilcraft.lib.v2.rendering.foundation.compound.DirtyTracked;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@SuppressWarnings({"FieldMayBeFinal", "SameParameterValue"})
public class BloomPostEffect implements DirtyTracked, AutoCloseable {
    public static final int UNIFORM_TRANSFORM_SIZE = TransformsUbo.DEFINITION.size(BufferLayout.STD140);
    public static final int UNIFORM_BLOOM_SIZE = BloomParametersUbo.DEFINITION.size(BufferLayout.STD140);
    public static final int UNIFORM_ENHANCED_BLOOM_SIZE = BloomPipelineParametersUbo.DEFINITION.size(BufferLayout.STD140);

    // todo: uses config or options
    private static final int PASSES_AMOUNT = 5;
    private static final int PASS_STEP = 1;

    @Getter
    private final RenderTarget bloomInputTarget = new MainTarget(854, 480);
    private final RenderTarget bloomTempTarget = new TextureTarget("BloomTemp", 854, 480, com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, null);

    private final RenderTarget[] downsampleTargets = arrayInit("DownSample", PASSES_AMOUNT);
    private final RenderTarget[] upsampleTargets = arrayInit("UpSample", PASSES_AMOUNT - 1);

    private final GpuDevice device = RenderSystem.getDevice();

    private final GpuBuffer transformUBO = device.createBuffer(
        () -> "BloomPostEffect TransformUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        UNIFORM_TRANSFORM_SIZE
    );

    private final GpuBuffer bloomUBO = device.createBuffer(
        () -> "BloomPostEffect BloomApplyUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        UNIFORM_BLOOM_SIZE
    );

    private final GpuBuffer enhancedBloomParametersUBO = device.createBuffer(
        () -> "BloomPostEffect BloomParametersUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        UNIFORM_ENHANCED_BLOOM_SIZE
    );

    private final GpuBuffer vertexBuffer = device.createBuffer(
        () -> "BloomPostEffect VertexBuffer",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX,
        1024
    );

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

    @Getter
    private final BloomParametersUbo bloomParameters;
    @Getter
    private final BloomPipelineParametersUbo enhancedBloomParameters;

    private final TransformsUbo transformsUbo = new TransformsUbo(new Matrix4f());
    private final List<BloomRenderCallback> bloomCalls = new ArrayList<>();
    @Getter
    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();

    private int width;
    private int height;
    private int indexCount;
    private boolean dirty = false;
    @Getter
    private boolean drawingBloom;

    private int passes;
    private int step;

    public BloomPostEffect() {
        this(1.25f, 1.943f, 1.105f, 0.08f, 0.8f, PASSES_AMOUNT, PASS_STEP);
    }

    public BloomPostEffect(
        float bloomIntensity,
        float sampleStepLength,
        float colorMultiplier,
        float bloomThreshold,
        float bloomIntensityMultiplier,
        int passes,
        int step
    ) {
        RenderTarget mainRenderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();

        this.width = mainRenderTarget.width;
        this.height = mainRenderTarget.height;
        this.bloomParameters = new BloomParametersUbo(bloomIntensity, bloomThreshold, bloomIntensityMultiplier);
        this.enhancedBloomParameters = new BloomPipelineParametersUbo();
        this.passes = passes;
        this.step = step;

        resize(width, height);
    }

    public void beginFrame() {
        clearColorAndDepth(bloomInputTarget, 0);
        clearColorAndDepth(bloomTempTarget, 0);
        dirty = false;
        bloomCalls.clear();
        FeatureRendering.clear(submitNodeStorage);
    }

    public void markDirty() {
        dirty = true;
    }

    public void drawBloomed(BloomRenderCallback runnable) {
        bloomCalls.add(runnable);
        markDirty();
    }

    public CompoundSubmitNodeStorage createCompoundSubmitStorage(SubmitNodeCollector collector) {
        return new CompoundSubmitNodeStorage(this.submitNodeStorage, collector, this);
    }

    public void beginBloomDraw() {
        RenderTarget mainRenderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();

        this.guardSize(mainRenderTarget);

        bloomInputTarget.copyDepthFrom(mainRenderTarget);

        setupOutputOverride();
    }

    private void guardSize(RenderTarget mainRenderTarget) {
        if (mainRenderTarget.width != this.bloomInputTarget.width || mainRenderTarget.height != this.bloomInputTarget.height) {
            this.resize(mainRenderTarget.width, mainRenderTarget.height);
        }
    }

    public void endBloomDraw() {
        teardownOutputOverride();
        // more cleanup needed here maybe
    }

    public void setupOutputOverride() {
        this.drawingBloom = true;
        this.markDirty();
    }

    public void teardownOutputOverride() {
        this.drawingBloom = false;
    }

    public void runBloomDraws(Matrix4fc modelViewMatrix, FeatureRenderDispatcher featureRenderDispatcher) {
        if (!dirty) return;

        PoseStack poseStack = new PoseStack();
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().set(modelViewMatrix);
        try {
            beginBloomDraw();
            if (!bloomCalls.isEmpty()) {
                for (BloomRenderCallback bloomCall : bloomCalls) {
                    bloomCall.render(this.submitNodeStorage, poseStack);
                }
            }
            FeatureRendering.draw(featureRenderDispatcher, this.submitNodeStorage, this.bloomInputTarget);
        } finally {
            endBloomDraw();
            RenderSystem.getModelViewStack().popMatrix();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void process() {
        if (!dirty) return;
        CommandEncoder commandEncoder = device.createCommandEncoder();
        RenderTarget mainRenderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();

        this.guardSize(mainRenderTarget);

        transformsUbo.upload(commandEncoder, transformUBO.slice());

        this.doDownSample(commandEncoder, bloomInputTarget);
        this.doUpSample(commandEncoder);

        // backup depth texture
        bloomInputTarget.copyDepthFrom(mainRenderTarget);

        clearColorAndDepth(bloomTempTarget, 0);
        applyBloom(commandEncoder, this.upsampleTargets[0], mainRenderTarget.getColorTextureView(), bloomTempTarget);
        commandEncoder.copyTextureToTexture(
            bloomTempTarget.getColorTexture(),
            mainRenderTarget.getColorTexture(),
            0,
            0,
            0,
            0,
            0,
            width,
            height
        );
        mainRenderTarget.copyDepthFrom(bloomInputTarget);
    }

    @SuppressWarnings("DataFlowIssue")
    private void clearColorAndDepth(RenderTarget rt, int color) {
        if (rt.hasDepth()) {
            device.createCommandEncoder().clearColorAndDepthTextures(rt.getColorTexture(), net.minecraft.util.ARGB.vector4fFromARGB32(color), rt.getDepthTexture(), 0);
        } else {
            device.createCommandEncoder().clearColorTexture(rt.getColorTexture(), net.minecraft.util.ARGB.vector4fFromARGB32(color));
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private void applyBloom(
        CommandEncoder commandEncoder,
        RenderTarget inputTarget,
        GpuTextureView gameTexture,
        RenderTarget outputTarget
    ) {
        transformsUbo.upload(commandEncoder, transformUBO.slice());
        bloomParameters.upload(commandEncoder, bloomUBO.slice());
        RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = sequentialBuffer.getBuffer(indexCount);
        try (RenderPass bloomPass = commandEncoder.createRenderPass(
            () -> "BloomPostEffect BloomApply",
            outputTarget.getColorTextureView(),
            Optional.of(new org.joml.Vector4f())
        )) {
            bloomPass.setPipeline(RenderSystem.getCompiledPipeline(ALRPipelines.APPLY_BLOOM));
            bloomPass.setUniform("Transforms", transformUBO);
            bloomPass.setUniform("BloomParameters", bloomUBO);
            bloomPass.setUniform("DiffuseSampler", inputTarget.getColorTextureView(), inputSampler);
            bloomPass.setUniform("GameSampler", gameTexture, mainSampler);
            bloomPass.setVertexBuffer(0, vertexBuffer.slice());
            bloomPass.setIndexBuffer(indexBuffer, sequentialBuffer.type());
            bloomPass.drawIndexed(indexCount, 1, 0, 0, 0);
        }
    }

    private void doDownSample(
        CommandEncoder commandEncoder,
        RenderTarget inputTarget
    ) {

        this.downSample(
            commandEncoder,
            inputTarget,
            this.downsampleTargets[0],
            0
        );

        for (int i = 1; i < this.passes; i++) {
            this.downSample(
                commandEncoder,
                this.downsampleTargets[i - 1],
                this.downsampleTargets[i],
                i
            );
        }

    }

    @SuppressWarnings("DataFlowIssue")
    private void downSample(
        CommandEncoder commandEncoder,
        RenderTarget src,
        RenderTarget dst,
        int frameIndex
    ) {
        this.enhancedBloomParameters.setFrameIndex(frameIndex);
        this.enhancedBloomParameters.setResolution(src.width, src.height);
        this.enhancedBloomParameters.upload(commandEncoder, enhancedBloomParametersUBO.slice());

        RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = sequentialBuffer.getBuffer(indexCount);
        try (RenderPass pass = commandEncoder.createRenderPass(
            () -> ("BloomPostEffect DownSample " + frameIndex),
            dst.getColorTextureView(),
            Optional.of(new org.joml.Vector4f())
        )) {

            pass.setPipeline(RenderSystem.getCompiledPipeline(ALRPipelines.DOWNSAMPLE));
            pass.setUniform("Transforms", transformUBO);
            pass.setUniform("BloomParameters", enhancedBloomParametersUBO);
            pass.setUniform("DiffuseSampler", src.getColorTextureView(), inputSampler);

            pass.setVertexBuffer(0, vertexBuffer.slice());
            pass.setIndexBuffer(indexBuffer, sequentialBuffer.type());
            pass.drawIndexed(indexCount, 1, 0, 0, 0);
        }
    }

    private void doUpSample(CommandEncoder commandEncoder) {
        var steps = this.passes;

        this.upSample(
            commandEncoder,
            this.downsampleTargets[steps - 2],
            this.downsampleTargets[steps - 1],
            this.upsampleTargets[steps - 2],
            steps - 1
        );

        for (int i = steps - 2; i > 0; i--) {
            this.upSample(
                commandEncoder,
                this.downsampleTargets[i - 1],
                this.upsampleTargets[i],
                this.upsampleTargets[i - 1],
                i
            );
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private void upSample(
        CommandEncoder commandEncoder,
        RenderTarget curr,
        RenderTarget prev,
        RenderTarget dst,
        int frameIndex
    ) {
        this.enhancedBloomParameters.setFrameIndex(frameIndex);
        this.enhancedBloomParameters.setResolution(curr.width, curr.height);
        this.enhancedBloomParameters.upload(commandEncoder, enhancedBloomParametersUBO.slice());

        RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = sequentialBuffer.getBuffer(indexCount);
        try (RenderPass pass = commandEncoder.createRenderPass(
            () -> ("BloomPostEffect UpSample " + frameIndex),
            dst.getColorTextureView(),
            Optional.of(new org.joml.Vector4f())
        )) {

            pass.setPipeline(RenderSystem.getCompiledPipeline(ALRPipelines.UPSAMPLE));
            pass.setUniform("Transforms", transformUBO);
            pass.setUniform("BloomParameters", enhancedBloomParametersUBO);
            pass.setUniform("DiffuseSampler", curr.getColorTextureView(), inputSampler);
            pass.setUniform("PreviousSampler", prev.getColorTextureView(), mainSampler);

            pass.setVertexBuffer(0, vertexBuffer.slice());
            pass.setIndexBuffer(indexBuffer, sequentialBuffer.type());
            pass.drawIndexed(indexCount, 1, 0, 0, 0);
        }
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        bloomInputTarget.resize(width, height);
        bloomTempTarget.resize(width, height);
        transformsUbo.getProjMat().setOrtho(
            0, width, 0, height, -1, -10000f, device.getDeviceInfo().isZZeroToOne()
        );

        try (ByteBufferBuilder memory = new ByteBufferBuilder(256)) {
        BufferBuilder builder = new BufferBuilder(memory, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);

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

        this.passes = PASSES_AMOUNT;

        int pWidth = width;
        int pHeight = height;
        int step = this.step;

        for (int i = 0; i < PASSES_AMOUNT; i++) {
            pWidth >>= step;
            pHeight >>= step;

            if (pWidth == 0 || pHeight == 0) {
                this.passes = i;
                break;
            }

            this.downsampleTargets[i].resize(pWidth, pHeight);

            if (i < PASSES_AMOUNT - 1) {
                this.upsampleTargets[i].resize(pWidth, pHeight);
            }
        }
    }

    private static RenderTarget[] arrayInit(String name, int size) {
        RenderTarget[] targets = new RenderTarget[size];

        for (int i = 0; i < size; i++) {
            var target = new TextureTarget(
                "Bloom_" + name + "_" + i,
                854 >> i, 480 >> i,
                com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, null
            );
            targets[i] = target;
        }

        return targets;
    }
    @Override
    public void close() {
        this.transformUBO.close();
        this.bloomUBO.close();
        this.enhancedBloomParametersUBO.close();
        this.vertexBuffer.close();
        this.inputSampler.close();
        this.mainSampler.close();
        this.bloomInputTarget.destroyBuffers();
        this.bloomTempTarget.destroyBuffers();
        for (RenderTarget target : this.downsampleTargets) target.destroyBuffers();
        for (RenderTarget target : this.upsampleTargets) target.destroyBuffers();
    }
}
