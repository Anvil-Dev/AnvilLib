package dev.anvilcraft.lib.v2.rendering.glitch;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.MeshData;
import dev.anvilcraft.lib.v2.rendering.bloom.TransformsUbo;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.postprocess.ALRPostEffectShaders;
import dev.anvilcraft.lib.v2.rendering.postprocess.GlPostProcess;
import dev.anvilcraft.lib.v2.rendering.postprocess.GlUniformBuffer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

/**
 * Time-driven glitch post-processing effect.
 * <p>
 * Ported from 26.1 (see #P7b): {@code process(GpuTextureView)} becomes
 * {@code process(RenderTarget, int, int)} returning a {@link RenderTarget}; the 26.1
 * {@code DeltaTracker} is replaced by {@code Minecraft.getTimer().getGameTimeDeltaPartialTick(false)}.
 * The shader and its std140 layout are unchanged.
 */
public class GlitchPostEffect {
    public static final int UNIFORM_GLITCH_SIZE = GlitchParametersUbo.DEFINITION.size(BufferLayout.STD140);
    public static final int UNIFORM_TRANSFORM_SIZE = TransformsUbo.DEFINITION.size(BufferLayout.STD140);

    @Getter
    private final RenderTarget glitchOutputTarget = new TextureTarget(854, 480, false, false);

    private final GlUniformBuffer transformUBO = new GlUniformBuffer(UNIFORM_TRANSFORM_SIZE);
    private final GlUniformBuffer glitchParameterUBO = new GlUniformBuffer(UNIFORM_GLITCH_SIZE);

    private final TransformsUbo transform = new TransformsUbo(new Matrix4f());
    private final GlitchParametersUbo glitchParameters = new GlitchParametersUbo();
    private int lastWidth = 0;
    private int lastHeight = 0;
    private MeshData quadMesh;

    public RenderTarget process(RenderTarget input, int width, int height) {
        if (glitchOutputTarget.width < width || glitchOutputTarget.height < height) {
            glitchOutputTarget.resize(width, height, false);
        }

        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level.getGameTime();
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        if (lastHeight != height || lastWidth != width) {
            this.lastWidth = width;
            this.lastHeight = height;
            this.quadMesh = GlPostProcess.buildQuad(width, height, true);
        }

        transform.getProjMat().setOrtho(
            0,
            glitchOutputTarget.width,
            0,
            glitchOutputTarget.height,
            -10000,
            10000
        );
        glitchParameters.getInSize().set(width, height);
        float uGameTime = (float) (gameTime % 24000L) + partialTick;
        uGameTime /= 10f;
        glitchParameters.setGameTime(uGameTime);

        transformUBO.upload(transform);
        glitchParameterUBO.upload(glitchParameters);

        ShaderInstance shader = ALRPostEffectShaders.getGlitchShader();
        if (shader == null) {
            return glitchOutputTarget;
        }
        GlPostProcess.beginTarget(glitchOutputTarget);
        shader.setSampler("DiffuseSampler", input);
        GlPostProcess.draw(shader, quadMesh, s -> {
            transformUBO.bindTo(s.getId(), "Transforms", 0);
            glitchParameterUBO.bindTo(s.getId(), "GlitchParameters", 1);
        });
        return glitchOutputTarget;
    }
}
