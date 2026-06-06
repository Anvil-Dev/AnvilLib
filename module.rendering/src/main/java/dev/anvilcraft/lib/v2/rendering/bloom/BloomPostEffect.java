package dev.anvilcraft.lib.v2.rendering.bloom;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline;
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
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.ALRPipelines;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.compound.CompoundSubmitNodeStorage;
import dev.anvilcraft.lib.v2.rendering.foundation.compound.DirtyTracked;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.client.pipeline.PipelineModifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@SuppressWarnings({"FieldMayBeFinal", "SameParameterValue"})
public class BloomPostEffect implements DirtyTracked {
    public static final int UNIFORM_TRANSFORM_SIZE = TransformsUbo.DEFINITION.size(BufferLayout.STD140);
    public static final int UNIFORM_BLOOM_SIZE = BloomParametersUbo.DEFINITION.size(BufferLayout.STD140);
    public static final int UNIFORM_ENHANCED_BLOOM_SIZE = BloomPipelineParametersUbo.DEFINITION.size(BufferLayout.STD140);

    // todo: uses config or options
    private static final int PASSES_AMOUNT = 5;
    private static final int PASS_STEP = 1;

    @Getter
    private final RenderTarget bloomInputTarget = new MainTarget(854, 480, false);
    private final RenderTarget bloomTempTarget = new TextureTarget("BloomTemp", 854, 480, false);

    private final RenderTarget[] downsampleTargets = arrayInit("DownSample", PASSES_AMOUNT);
    private final RenderTarget[] upsampleTargets = arrayInit("UpSample", PASSES_AMOUNT - 1);

    private final GpuDevice device = RenderSystem.getDevice();

    private final GpuBuffer transformUBO = device.createBuffer(
        () -> "BloomPostEffect->TransformUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        UNIFORM_TRANSFORM_SIZE
    );

    private final GpuBuffer bloomUBO = device.createBuffer(
        () -> "BloomPostEffect->BloomApplyUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        UNIFORM_BLOOM_SIZE
    );

    private final GpuBuffer enhancedBloomParametersUBO = device.createBuffer(
        () -> "BloomPostEffect->BloomParametersUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        UNIFORM_ENHANCED_BLOOM_SIZE
    );

    private final GpuBuffer vertexBuffer = device.createBuffer(
        () -> "BloomPostEffect->VertexBuffer",
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
        Window window = Minecraft.getInstance().getWindow();

        this.width = window.getWidth();
        this.height = window.getHeight();
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
        submitNodeStorage.clear();
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
        setupOutputOverride();
        bloomInputTarget.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
    }

    public void endBloomDraw() {
        teardownOutputOverride();
        // more cleanup needed here maybe
    }

    public void setupOutputOverride() {
        RenderSystem.outputColorTextureOverride = bloomInputTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = bloomInputTarget.getDepthTextureView();
        this.markDirty();
    }

    public void teardownOutputOverride() {
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
    }

    public void runBloomDraws(Matrix4fc modelViewMatrix, FeatureRenderDispatcher featureRenderDispatcher) {
        if (!dirty) return;
        beginBloomDraw();
        PoseStack poseStack = new PoseStack();
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().set(modelViewMatrix);
        if (!bloomCalls.isEmpty()) {
            for (BloomRenderCallback bloomCall : bloomCalls) {
                bloomCall.render(featureRenderDispatcher.getSubmitNodeStorage(), poseStack);
            }
        }
        featureRenderDispatcher.renderAllFeatures();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        RenderSystem.getModelViewStack().popMatrix();
        endBloomDraw();
    }

    @SuppressWarnings("DataFlowIssue")
    public void process() {
        if (!dirty) return;
        CommandEncoder commandEncoder = device.createCommandEncoder();

        transformsUbo.upload(commandEncoder, transformUBO.slice());

        this.doDownSample(commandEncoder, bloomInputTarget);
        this.doUpSample(commandEncoder);

        // backup depth texture
        bloomInputTarget.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());

        clearColorAndDepth(bloomTempTarget, 0);
        applyBloom(commandEncoder, this.upsampleTargets[0], Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), bloomTempTarget);
        commandEncoder.copyTextureToTexture(
            bloomTempTarget.getColorTexture(),
            Minecraft.getInstance().getMainRenderTarget().getColorTexture(),
            0,
            0,
            0,
            0,
            0,
            width,
            height
        );
        Minecraft.getInstance().getMainRenderTarget().copyDepthFrom(bloomInputTarget);
    }

    @SuppressWarnings("DataFlowIssue")
    private void clearColorAndDepth(RenderTarget rt, int color) {
        if (rt.useDepth) {
            device.createCommandEncoder().clearColorAndDepthTextures(rt.getColorTexture(), color, rt.getDepthTexture(), 1);
        } else {
            device.createCommandEncoder().clearColorTexture(rt.getColorTexture(), color);
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
        RenderPass bloomPass = commandEncoder.createRenderPass(
            () -> "BloomPostEffect BloomApply",
            outputTarget.getColorTextureView(),
            OptionalInt.of(0)
        );
        bloomPass.setPipeline(ALRPipelines.APPLY_BLOOM);
        bloomPass.setUniform("Transforms", transformUBO);
        bloomPass.setUniform("BloomParameters", bloomUBO);
        bloomPass.bindTexture("DiffuseSampler", inputTarget.getColorTextureView(), inputSampler);
        bloomPass.bindTexture("GameSampler", gameTexture, mainSampler);
        bloomPass.setVertexBuffer(0, vertexBuffer);
        RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        bloomPass.setIndexBuffer(sequentialBuffer.getBuffer(indexCount), sequentialBuffer.type());
        bloomPass.drawIndexed(0, 0, indexCount, 1);
        bloomPass.close();
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

        var pass = commandEncoder.createRenderPass(
            () -> ("BloomPostEffect DownSample " + frameIndex),
            dst.getColorTextureView(),
            OptionalInt.of(0)
        );

        pass.setPipeline(ALRPipelines.DOWNSAMPLE);
        pass.setUniform("Transforms", transformUBO);
        pass.setUniform("BloomParameters", enhancedBloomParametersUBO);
        pass.bindTexture("DiffuseSampler", src.getColorTextureView(), inputSampler);

        pass.setVertexBuffer(0, vertexBuffer);
        RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        pass.setIndexBuffer(sequentialBuffer.getBuffer(indexCount), sequentialBuffer.type());
        pass.drawIndexed(0, 0, indexCount, 1);
        pass.close();
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

        var pass = commandEncoder.createRenderPass(
            () -> ("BloomPostEffect UpSample " + frameIndex),
            dst.getColorTextureView(),
            OptionalInt.of(0)
        );

        pass.setPipeline(ALRPipelines.UPSAMPLE);
        pass.setUniform("Transforms", transformUBO);
        pass.setUniform("BloomParameters", enhancedBloomParametersUBO);
        pass.bindTexture("DiffuseSampler", curr.getColorTextureView(), inputSampler);
        pass.bindTexture("PreviousSampler", prev.getColorTextureView(), mainSampler);

        pass.setVertexBuffer(0, vertexBuffer);
        RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        pass.setIndexBuffer(sequentialBuffer.getBuffer(indexCount), sequentialBuffer.type());
        pass.drawIndexed(0, 0, indexCount, 1);
        pass.close();
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        bloomInputTarget.resize(width, height);
        bloomTempTarget.resize(width, height);
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
                false
            );
            targets[i] = target;
        }

        return targets;
    }
}
