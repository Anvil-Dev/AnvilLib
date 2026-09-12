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

@ApiStatus.Internal
public record RingRenderState(
    Matrix3x2f pose,
    float x0,
    float y0,
    float x1,
    float y1,
    int color,
    GpuBufferSlice ringUniform,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements LibQuadGuiElementRenderState {
    public RingRenderState(
        Matrix3x2f pose,
        float x0,
        float y0,
        float x1,
        float y1,
        int color,
        GpuBufferSlice ringUniform,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(
            pose,
            x0,
            y0,
            x1,
            y1,
            color,
            ringUniform,
            scissorArea,
            LibGuiElementRenderState.getBounds(pose, x0, y0, x1, y1, scissorArea)
        );
    }

    @Override
    public RenderPipeline pipeline() {
        return LibRenders.RING_PIPELINE;
    }

    @Override
    public Map<String, GpuBufferSlice> bufferSlices() {
        return Map.of("RingUniform", this.ringUniform());
    }
}
