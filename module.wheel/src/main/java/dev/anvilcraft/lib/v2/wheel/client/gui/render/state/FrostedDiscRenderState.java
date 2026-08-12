package dev.anvilcraft.lib.v2.wheel.client.gui.render.state;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.state.LibGuiElementRenderState;
import dev.anvilcraft.lib.v2.rendering.state.LibQuadGuiElementRenderState;
import dev.anvilcraft.lib.v2.wheel.client.init.LibRenders;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * 轮盘毛玻璃盘面：采样主渲染目标的高斯模糊结果，并用圆形遮罩裁出盘面区域。
 * 纹理 V 轴在 GUI 采样时翻转，顶点颜色用于调制模糊内容与叠加透明度。
 */
@ApiStatus.Internal
public record FrostedDiscRenderState(
    Matrix3x2f pose,
    float x0,
    float y0,
    float x1,
    float y1,
    int color,
    GpuBufferSlice frostedDiscUniform,
    TextureSetup textureSetup,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements LibQuadGuiElementRenderState {
    public FrostedDiscRenderState(
        Matrix3x2f pose,
        float x0,
        float y0,
        float x1,
        float y1,
        int color,
        GpuBufferSlice frostedDiscUniform,
        TextureSetup textureSetup,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(
            pose,
            x0,
            y0,
            x1,
            y1,
            color,
            frostedDiscUniform,
            textureSetup,
            scissorArea,
            LibGuiElementRenderState.getBounds(pose, x0, y0, x1, y1, scissorArea)
        );
    }

    @Override
    public RenderPipeline pipeline() {
        return LibRenders.FROSTED_DISC_PIPELINE;
    }

    @Override
    public Map<String, GpuBufferSlice> bufferSlices() {
        return Map.of("FrostedDiscUniform", this.frostedDiscUniform());
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        // 渲染目标纹理 v=0 对应画面底部，这里从顶部(GUI y0)开始取 v=1。
        consumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setUv(0f, 1f).setColor(this.color());
        consumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setUv(0f, 0f).setColor(this.color());
        consumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setUv(1f, 0f).setColor(this.color());
        consumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setUv(1f, 1f).setColor(this.color());
    }
}
