package dev.anvilcraft.lib.v2.rendering.bloom;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.compound.DirtyTracked;
import dev.anvilcraft.lib.v2.rendering.postprocess.ALRPostEffectShaders;
import dev.anvilcraft.lib.v2.rendering.postprocess.GlPostProcess;
import dev.anvilcraft.lib.v2.rendering.postprocess.GlUniformBuffer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Bloom post-processing effect: captures a re-rendered copy of the scene, runs it through a
 * 5-level downsample/upsample Gaussian pyramid and composites the result back onto the main target.
 * <p>
 * Ported from 26.1 (see #P7b): the {@code GpuDevice}/{@code CommandEncoder}/{@code RenderPass}
 * execution layer becomes {@link RenderTarget} + {@link ShaderInstance} (registered via
 * {@code RegisterShadersEvent}, see {@link ALRPostEffectShaders}) + fullscreen quad
 * ({@code BufferUploader}/{@code Tesselator}); UBO uploads use {@link GlUniformBuffer}.
 * The 26.1 {@code SubmitNodeStorage} redraw path is replaced by a {@link MultiBufferSource.BufferSource}
 * redraw; {@code BloomSubmitNodeStorage} itself is not portable and is intentionally dropped.
 */
@SuppressWarnings({"FieldMayBeFinal", "SameParameterValue"})
public class BloomPostEffect implements DirtyTracked {
    public static final int UNIFORM_TRANSFORM_SIZE = TransformsUbo.DEFINITION.size(BufferLayout.STD140);
    public static final int UNIFORM_BLOOM_SIZE = BloomParametersUbo.DEFINITION.size(BufferLayout.STD140);
    public static final int UNIFORM_ENHANCED_BLOOM_SIZE = BloomPipelineParametersUbo.DEFINITION.size(BufferLayout.STD140);

    // todo: uses config or options
    private static final int PASSES_AMOUNT = 5;
    private static final int PASS_STEP = 1;

    @Getter
    private final RenderTarget bloomInputTarget = new MainTarget(854, 480);
    private final RenderTarget bloomTempTarget = new TextureTarget(854, 480, false, false);

    private final RenderTarget[] downsampleTargets = arrayInit("DownSample", PASSES_AMOUNT);
    private final RenderTarget[] upsampleTargets = arrayInit("UpSample", PASSES_AMOUNT - 1);

    private final GlUniformBuffer transformUBO = new GlUniformBuffer(UNIFORM_TRANSFORM_SIZE);
    private final GlUniformBuffer bloomUBO = new GlUniformBuffer(UNIFORM_BLOOM_SIZE);
    private final GlUniformBuffer enhancedBloomParametersUBO = new GlUniformBuffer(UNIFORM_ENHANCED_BLOOM_SIZE);

    @Getter
    private final BloomParametersUbo bloomParameters;
    @Getter
    private final BloomPipelineParametersUbo enhancedBloomParameters;

    private final TransformsUbo transformsUbo = new TransformsUbo(new Matrix4f());
    private final List<BloomRenderCallback> bloomCalls = new ArrayList<>();

    private int width;
    private int height;
    private MeshData quadMesh;
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
        RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();

        this.width = mainRenderTarget.width;
        this.height = mainRenderTarget.height;
        this.bloomParameters = new BloomParametersUbo(bloomIntensity, bloomThreshold, bloomIntensityMultiplier);
        this.enhancedBloomParameters = new BloomPipelineParametersUbo();
        this.passes = passes;
        this.step = step;

        resize(width, height);
    }

    public void beginFrame() {
        clearColorAndDepth(bloomInputTarget);
        clearColorAndDepth(bloomTempTarget);
        dirty = false;
        bloomCalls.clear();
    }

    public void markDirty() {
        dirty = true;
    }

    public void drawBloomed(BloomRenderCallback runnable) {
        bloomCalls.add(runnable);
        markDirty();
    }

    public void beginBloomDraw() {
        RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();

        this.guardSize(mainRenderTarget);

        bloomInputTarget.copyDepthFrom(mainRenderTarget);

        bloomInputTarget.bindWrite(true);
        this.markDirty();
    }

    private void guardSize(RenderTarget mainRenderTarget) {
        if (mainRenderTarget.width != this.bloomInputTarget.width || mainRenderTarget.height != this.bloomInputTarget.height) {
            this.resize(mainRenderTarget.width, mainRenderTarget.height);
        }
    }

    public void endBloomDraw() {
        // restore the level's framebuffer
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    public void runBloomDraws(Matrix4f modelViewMatrix) {
        if (!dirty) return;

        PoseStack poseStack = new PoseStack();
        try {
            beginBloomDraw();
            if (!bloomCalls.isEmpty()) {
                try (com.mojang.blaze3d.vertex.ByteBufferBuilder bufferBuilder = new com.mojang.blaze3d.vertex.ByteBufferBuilder(256)) {
                    MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(bufferBuilder);
                    for (BloomRenderCallback bloomCall : bloomCalls) {
                        bloomCall.render(bufferSource, poseStack);
                    }
                    bufferSource.endBatch();
                }
            }
        } finally {
            endBloomDraw();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void process() {
        if (!dirty) return;
        RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();

        this.guardSize(mainRenderTarget);

        transformUBO.upload(transformsUbo);

        this.doDownSample();
        this.doUpSample();

        // backup depth texture
        bloomInputTarget.copyDepthFrom(mainRenderTarget);

        clearColorAndDepth(bloomTempTarget);
        applyBloom(this.upsampleTargets[0], mainRenderTarget, bloomTempTarget);
        copyColor(bloomTempTarget, mainRenderTarget);
        mainRenderTarget.copyDepthFrom(bloomInputTarget);
    }

    private void clearColorAndDepth(RenderTarget rt) {
        rt.clear(false);
    }

    /**
     * Copies the color attachment of {@code src} onto the color texture of {@code dst} through the
     * GL read framebuffer. Replaces the 26.1 {@code CommandEncoder.copyTextureToTexture}.
     */
    private void copyColor(RenderTarget src, RenderTarget dst) {
        GlStateManager._activeTexture(33984);
        GlStateManager._glBindFramebuffer(36008, src.frameBufferId);
        GlStateManager._bindTexture(dst.getColorTextureId());
        GlStateManager._glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, this.width, this.height);
        GlStateManager._bindTexture(0);
        GlStateManager._glBindFramebuffer(36008, 0);
    }

    private void applyBloom(
        RenderTarget inputTarget,
        RenderTarget gameTarget,
        RenderTarget outputTarget
    ) {
        ShaderInstance shader = ALRPostEffectShaders.getApplyBloomShader();
        if (shader == null) return;
        transformUBO.upload(transformsUbo);
        bloomUBO.upload(bloomParameters);
        GlPostProcess.beginTarget(outputTarget);
        shader.setSampler("GameSampler", gameTarget);
        shader.setSampler("DiffuseSampler", inputTarget);
        GlPostProcess.draw(shader, quadMesh, s -> {
            transformUBO.bindTo(s.getId(), "Transforms", 0);
            bloomUBO.bindTo(s.getId(), "BloomParameters", 1);
        });
    }

    private void doDownSample() {

        this.downSample(
            this.bloomInputTarget,
            this.downsampleTargets[0],
            0
        );

        for (int i = 1; i < this.passes; i++) {
            this.downSample(
                this.downsampleTargets[i - 1],
                this.downsampleTargets[i],
                i
            );
        }

    }

    private void downSample(
        RenderTarget src,
        RenderTarget dst,
        int frameIndex
    ) {
        ShaderInstance shader = ALRPostEffectShaders.getDownSampleShader();
        if (shader == null) return;
        this.enhancedBloomParameters.setFrameIndex(frameIndex);
        this.enhancedBloomParameters.setResolution(src.width, src.height);
        this.enhancedBloomParametersUBO.upload(enhancedBloomParameters);

        GlPostProcess.beginTarget(dst);
        shader.setSampler("DiffuseSampler", src);
        GlPostProcess.draw(shader, quadMesh, s -> {
            transformUBO.bindTo(s.getId(), "Transforms", 0);
            enhancedBloomParametersUBO.bindTo(s.getId(), "BloomParameters", 1);
        });
    }

    private void doUpSample() {
        var steps = this.passes;

        this.upSample(
            this.downsampleTargets[steps - 2],
            this.downsampleTargets[steps - 1],
            this.upsampleTargets[steps - 2],
            steps - 1
        );

        for (int i = steps - 2; i > 0; i--) {
            this.upSample(
                this.downsampleTargets[i - 1],
                this.upsampleTargets[i],
                this.upsampleTargets[i - 1],
                i
            );
        }
    }

    private void upSample(
        RenderTarget curr,
        RenderTarget prev,
        RenderTarget dst,
        int frameIndex
    ) {
        ShaderInstance shader = ALRPostEffectShaders.getUpSampleShader();
        if (shader == null) return;
        this.enhancedBloomParameters.setFrameIndex(frameIndex);
        this.enhancedBloomParameters.setResolution(curr.width, curr.height);
        this.enhancedBloomParametersUBO.upload(enhancedBloomParameters);

        GlPostProcess.beginTarget(dst);
        shader.setSampler("DiffuseSampler", curr);
        shader.setSampler("PreviousSampler", prev);
        GlPostProcess.draw(shader, quadMesh, s -> {
            transformUBO.bindTo(s.getId(), "Transforms", 0);
            enhancedBloomParametersUBO.bindTo(s.getId(), "BloomParameters", 1);
        });
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        bloomInputTarget.resize(width, height, false);
        bloomTempTarget.resize(width, height, false);
        transformsUbo.getProjMat().setOrtho(
            0, width, 0, height, -1, -10000f
        );

        this.quadMesh = GlPostProcess.buildQuad(width, height, false);

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

            this.downsampleTargets[i].resize(pWidth, pHeight, false);

            if (i < PASSES_AMOUNT - 1) {
                this.upsampleTargets[i].resize(pWidth, pHeight, false);
            }
        }
    }

    private static RenderTarget[] arrayInit(String name, int size) {
        RenderTarget[] targets = new RenderTarget[size];

        for (int i = 0; i < size; i++) {
            var target = new TextureTarget(
                854 >> i, 480 >> i,
                false, false
            );
            targets[i] = target;
        }

        return targets;
    }
}
