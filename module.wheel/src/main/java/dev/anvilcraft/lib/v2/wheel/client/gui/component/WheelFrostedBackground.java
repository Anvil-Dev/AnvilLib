package dev.anvilcraft.lib.v2.wheel.client.gui.component;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.anvilcraft.lib.v2.rendering.blur.GaussianBlur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalDouble;
import javax.annotation.Nullable;

/**
 * 轮盘毛玻璃盘面背景：每帧对主渲染目标做一次高斯模糊，供盘面着色器采样。
 * 模糊结果纹理对象是复用的，因此只创建一次纹理视图。
 */
@ApiStatus.Internal
public final class WheelFrostedBackground implements AutoCloseable {
    private final GaussianBlur blur = new GaussianBlur(2.0f, 1.0f);
    private final GpuSampler sampler = RenderSystem.getDevice().createSampler(
        AddressMode.CLAMP_TO_EDGE,
        AddressMode.CLAMP_TO_EDGE,
        FilterMode.LINEAR,
        FilterMode.LINEAR,
        1,
        OptionalDouble.empty()
    );

    private int width;
    private int height;
    @Nullable
    private GpuTexture blurredTexture;
    @Nullable
    private GpuTextureView blurredTextureView;

    /**
     * 对当前主渲染目标执行模糊，并返回绑定模糊结果的纹理设置。
     *
     * @return 可直接用于 GUI 元素渲染的 TextureSetup
     */
    public TextureSetup capture() {
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        this.guardSize(mainTarget);
        GpuTexture blurred = this.blur.process(mainTarget.getColorTexture());
        if (this.blurredTextureView == null || this.blurredTexture != blurred) {
            if (this.blurredTextureView != null) {
                this.blurredTextureView.close();
            }
            this.blurredTexture = blurred;
            this.blurredTextureView = RenderSystem.getDevice().createTextureView(blurred);
        }
        return TextureSetup.singleTexture(this.blurredTextureView, this.sampler);
    }

    private void guardSize(RenderTarget mainTarget) {
        if (mainTarget.width == this.width && mainTarget.height == this.height) {
            return;
        }
        this.width = mainTarget.width;
        this.height = mainTarget.height;
        this.blur.resize(this.width, this.height);
    }

    @Override
    public void close() {
        this.sampler.close();
        if (this.blurredTextureView != null) {
            this.blurredTextureView.close();
            this.blurredTextureView = null;
        }
        this.blurredTexture = null;
        this.blur.close();
    }
}
