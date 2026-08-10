package dev.anvilcraft.lib.v2.rendering.blur;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
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
import org.joml.Vector2f;

/**
 * Two-pass (horizontal + vertical) Gaussian blur over a {@link RenderTarget}.
 * <p>
 * Ported from 26.1 (see #P7b): the {@code GpuDevice}/{@code CommandEncoder}/{@code RenderPass}
 * execution layer becomes {@link RenderTarget} + {@link ShaderInstance} + fullscreen quad;
 * UBO uploads use {@link GlUniformBuffer}. The shader and its std140 layout are unchanged.
 */
public class GaussianBlur implements AutoCloseable {
    private final RenderTarget inputTarget = new MainTarget(854, 480);
    private final RenderTarget tempTarget = new TextureTarget(854, 480, false, false);

    private final GlUniformBuffer blurUBO = new GlUniformBuffer(BlurParametersUbo.DEFINITION.size(BufferLayout.STD140));
    private final GlUniformBuffer transformUBO = new GlUniformBuffer(TransformsUbo.DEFINITION.size(BufferLayout.STD140));

    @Getter
    private final BlurParametersUbo blurParameters;

    private final TransformsUbo transformsUbo = new TransformsUbo(new Matrix4f());

    private int width;
    private int height;
    private MeshData quadMesh;

    public GaussianBlur(float sampleStepLength, float colorMultiplier) {
        Window window = Minecraft.getInstance().getWindow();

        this.width = window.getWidth();
        this.height = window.getHeight();
        this.blurParameters = new BlurParametersUbo(sampleStepLength, colorMultiplier, new Vector2f());

        resize(width, height);
    }

    /**
     * Runs two horizontal and two vertical blur passes and returns the blurred target.
     */
    public RenderTarget process(RenderTarget input) {
        transformUBO.upload(transformsUbo);

        blurOnce(input, tempTarget, true);
        blurOnce(tempTarget, inputTarget, true);

        blurOnce(inputTarget, tempTarget, false);
        blurOnce(tempTarget, inputTarget, false);

        return inputTarget;
    }

    private void blurOnce(RenderTarget inputTarget, RenderTarget outputTarget, boolean horizontal) {
        ShaderInstance shader = ALRPostEffectShaders.getBlurShader();
        if (shader == null) return;
        Vector2f direction;
        if (horizontal) {
            direction = new Vector2f(1f, 0f);
        } else {
            direction = new Vector2f(0f, 1f);
        }
        blurParameters.setDirection(direction);
        blurUBO.upload(blurParameters);

        GlPostProcess.beginTarget(outputTarget);
        shader.setSampler("DiffuseSampler", inputTarget);
        GlPostProcess.draw(shader, quadMesh, s -> {
            transformUBO.bindTo(s.getId(), "Transforms", 0);
            blurUBO.bindTo(s.getId(), "BlurParameters", 1);
        });
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        inputTarget.resize(width, height, false);
        tempTarget.resize(width, height, false);
        transformsUbo.getProjMat().setOrtho(
            0, width, 0, height, -1, -10000f
        );

        this.quadMesh = GlPostProcess.buildQuad(width, height, false);
    }

    @Override
    public void close() {
        blurUBO.close();
        transformUBO.close();
        inputTarget.destroyBuffers();
        tempTarget.destroyBuffers();
    }
}
