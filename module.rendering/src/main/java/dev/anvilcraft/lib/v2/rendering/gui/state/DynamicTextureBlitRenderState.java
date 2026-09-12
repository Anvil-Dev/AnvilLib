package dev.anvilcraft.lib.v2.rendering.gui.state;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

@ApiStatus.Internal
public record DynamicTextureBlitRenderState(
    RenderPipeline pipeline,
    Supplier<TextureSetup> textureSetupSupplier,
    Matrix3x2f pose,
    int x0,
    int y0,
    int x1,
    int y1,
    float u0,
    float v0,
    float u1,
    float v1,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public DynamicTextureBlitRenderState(
        RenderPipeline pipeline,
        Supplier<TextureSetup> textureSetup,
        Matrix3x2f pose,
        int x0,
        int y0,
        int x1,
        int y1,
        float u0,
        float v0,
        float u1,
        float v1,
        int color,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(
            pipeline,
            textureSetup,
            pose,
            x0,
            y0,
            x1,
            y1,
            u0,
            v0,
            u1,
            v1,
            color,
            scissorArea,
            getBounds(x0, y0, x1, y1, pose, scissorArea)
        );
    }

    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(this.pose(), (float) this.x0(), (float) this.y0()).setUv(
            this.u0(),
            this.v0()
        ).setColor(this.color());
        vertexConsumer.addVertexWith2DPose(this.pose(), (float) this.x0(), (float) this.y1()).setUv(
            this.u0(),
            this.v1()
        ).setColor(this.color());
        vertexConsumer.addVertexWith2DPose(this.pose(), (float) this.x1(), (float) this.y1()).setUv(
            this.u1(),
            this.v1()
        ).setColor(this.color());
        vertexConsumer.addVertexWith2DPose(this.pose(), (float) this.x1(), (float) this.y0()).setUv(
            this.u1(),
            this.v0()
        ).setColor(this.color());
    }

    @Override
    public TextureSetup textureSetup() {
        return textureSetupSupplier.get();
    }

    private static @Nullable ScreenRectangle getBounds(
        int x0,
        int y0,
        int x1,
        int y1,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea
    ) {
        ScreenRectangle bounds = (new ScreenRectangle(x0, y0, x1 - x0, y1 - y0)).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }
}

