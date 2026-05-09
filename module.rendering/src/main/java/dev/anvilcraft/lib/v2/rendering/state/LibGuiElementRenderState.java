package dev.anvilcraft.lib.v2.rendering.state;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

import java.util.Map;
import javax.annotation.Nullable;

public interface LibGuiElementRenderState extends GuiElementRenderState {
    default Map<String, GpuBufferSlice> bufferSlices() {
        return Map.of();
    }

    default void executeDrawBeforeSetPipline(RenderPass renderPass) {
        this.bufferSlices().forEach(renderPass::setUniform);
    }

    default void executeDrawAfterSetPipline(RenderPass renderPass) {
    }

    static @Nullable ScreenRectangle getBounds(
        Matrix3x2f pose,
        float x0,
        float y0,
        float x1,
        float y1,
        @Nullable ScreenRectangle scissorArea
    ) {
        ScreenRectangle screenrectangle = new ScreenRectangle(
            Math.round(x0),
            Math.round(y0),
            Math.round(x1 - x0),
            Math.round(y1 - y0)
        ).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(screenrectangle) : screenrectangle;
    }
}
