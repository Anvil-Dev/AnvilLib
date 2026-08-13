package dev.anvilcraft.lib.v2.wheel.client.gui.component;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.wheel.client.init.LibShaders;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;

import java.util.Objects;

/**
 * 轮盘毛玻璃盘面背景：每帧对主渲染目标做一次可分离高斯模糊，供盘面着色器采样。
 * 两个纹理目标乒乓交替，水平/垂直各模糊两轮共四 pass，与 dev/26.1 的 GaussianBlur(2.0f, 1.0f) 行为一致。
 */
@ApiStatus.Internal
@Slf4j
public final class WheelFrostedBackground implements AutoCloseable {
    private static final float SAMPLE_STEP_LENGTH = 2.0f;

    private TextureTarget firstTarget = new TextureTarget(2, 2, true, true);
    private TextureTarget secondTarget = new TextureTarget(2, 2, true, true);
    private int width;
    private int height;

    /**
     * 对当前主渲染目标执行模糊，返回模糊结果纹理 id；blur shader 未就绪时返回 -1。
     */
    public int capture(GuiGraphics guiGraphics) {
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        this.guardSize(mainTarget);
        return this.blur(guiGraphics, mainTarget.getColorTextureId());
    }

    private int blur(GuiGraphics guiGraphics, int inputTextureId) {
        this.blurOnce(guiGraphics, this.secondTarget, inputTextureId, 1.0f, 0.0f);
        this.blurOnce(guiGraphics, this.firstTarget, this.secondTarget.getColorTextureId(), 0.0f, 1.0f);
        this.blurOnce(guiGraphics, this.secondTarget, this.firstTarget.getColorTextureId(), 1.0f, 0.0f);
        this.blurOnce(guiGraphics, this.firstTarget, this.secondTarget.getColorTextureId(), 0.0f, 1.0f);
        return this.firstTarget.getColorTextureId();
    }

    private void blurOnce(
        GuiGraphics guiGraphics,
        TextureTarget target,
        int inputTextureId,
        float directionX,
        float directionY
    ) {
        ShaderInstance blurShader = LibShaders.getBlurShader();
        if (blurShader == null) {
            log.error("Wheel frosted background blur shader is not ready");
            return;
        }
        target.bindWrite(false);
        RenderSystem.setShaderTexture(0, inputTextureId);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        float w = (float) Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float h = (float) Minecraft.getInstance().getWindow().getGuiScaledHeight();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        // 绘制到 FBO 时 GUI 顶部 (y=0) 对应纹理 v=1（纹理数据顶部），此处做 V 轴翻转
        bufferBuilder.addVertex(matrix4f, 0, 0, 0).setUv(0, 1);
        bufferBuilder.addVertex(matrix4f, 0, h, 0).setUv(0, 0);
        bufferBuilder.addVertex(matrix4f, w, h, 0).setUv(1, 0);
        bufferBuilder.addVertex(matrix4f, w, 0, 0).setUv(1, 1);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> blurShader);
        blurShader.safeGetUniform("Direction").set(directionX, directionY);
        blurShader.safeGetUniform("SampleStepLength").set(SAMPLE_STEP_LENGTH);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
        RenderSystem.enableDepthTest();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    private void guardSize(RenderTarget mainTarget) {
        if (mainTarget.width == this.width && mainTarget.height == this.height) {
            return;
        }
        this.width = mainTarget.width;
        this.height = mainTarget.height;
        this.firstTarget.destroyBuffers();
        this.secondTarget.destroyBuffers();
        this.firstTarget = new TextureTarget(this.width, this.height, true, true);
        this.secondTarget = new TextureTarget(this.width, this.height, true, true);
    }

    @Override
    public void close() {
        this.firstTarget.destroyBuffers();
        this.secondTarget.destroyBuffers();
    }
}
