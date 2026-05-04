package dev.anvilcraft.lib.v2.rendering.sdf;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.ALRPipelines;
import dev.anvilcraft.lib.v2.rendering.ALRendering;
import dev.anvilcraft.lib.v2.rendering.state.LibGuiElementRenderState;
import dev.anvilcraft.lib.v2.rendering.state.LibQuadGuiElementRenderState;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@EventBusSubscriber(modid = ALRendering.MODID, value = Dist.CLIENT)
public final class SdfGraphics {

    @SubscribeEvent
    public static void init(ConfigureMainRenderTargetEvent event) {
        SdfGraphics.instance = new SdfGraphics();
    }

    @SubscribeEvent
    public static void endFrame(RenderGuiEvent.Post event) {
        SdfGraphics.instance.flush();
    }

    private static final long       SDF_PARAMETER_SIZE          = SdfParametersUbo.DEFINITION.size();

    @Getter
    private static SdfGraphics      instance;

    private final GpuDevice         device                      = RenderSystem.getDevice();
    private final SdfParametersUbo  parameters                  = new SdfParametersUbo();
    private final CommandEncoder    encoder                     = this.device.createCommandEncoder();
    private final GpuBuffer         ubo                         = this.device.createBuffer(
            () -> "SDF Parameters",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
            SDF_PARAMETER_SIZE * 128L
    );

    private float               x, y, w, h;
    private int                 color;

    private float               smooth;
    private float               round;
    private float               rotation;

    private boolean             center;
    private int                 index;

    public SdfGraphics box(float x, float y, float width, float height) {
        this.x          = x;
        this.y          = y;
        this.w          = width;
        this.h          = height;

        this.parameters .box(this.w, this.h);

        return          this;
    }

    public SdfGraphics roundedHBar(float x, float y, float width, float height) {
        return          this.round(height * 0.5f)
                            .box(x, y, width, height);
    }

    public SdfGraphics circle(float x, float y, float radius) {
        this.x          = x;
        this.y          = y;
        this.w          = radius * 2;
        this.h          = radius * 2;

        this.parameters .circle(radius);

        return          this;
    }

    public SdfGraphics arc(float x, float y, float sweep, float radius, float width) {
        this.x          = x;
        this.y          = y;
        this.w          = radius * 2 + width;
        this.h          = radius * 2 + width;

        this.parameters .arc(sweep, radius, width);

        return          this;
    }

    public SdfGraphics sector(float x, float y, float sweep, float radius, float width) {
        this.x              = x;
        this.y              = y;
        this.w              = radius * 2;
        this.h              = radius * 2;

        this.parameters .sector(sweep, radius, width);

        return          this;
    }

    public SdfGraphics pie(float x, float y, float sweep, float radius) {
        this.x          = x;
        this.y          = y;
        this.w          = radius * 2;
        this.h          = radius * 2;

        this.parameters .pie(sweep, radius);

        return          this;
    }

    public SdfGraphics color(int color) {
        this.color      = color;
        return          this;
    }

    public SdfGraphics color(float red, float green, float blue, float alpha) {
        this.color      = ARGB.colorFromFloat(red, green, blue, alpha);
        return          this;
    }

    public SdfGraphics color(int red, int green, int blue, int alpha) {
        this.color      = ARGB.color(alpha, red, green, blue);
        return          this;
    }

    public SdfGraphics smooth(float radius) {
        this.smooth     = Math.max(0.0f, radius);
        return          this;
    }

    public SdfGraphics round(float radius) {
        this.round      = Math.max(0.0f, radius);
        return          this;
    }

    public SdfGraphics rotate(float degrees) {
        this.rotation   = Mth.wrapDegrees(degrees);
        return          this;
    }

    public SdfGraphics center(boolean center) {
        this.center     = center;
        return          this;
    }

    public SdfGraphics fill(@NotNull GuiGraphicsExtractor graphics) {
        this.parameters .shared(this.smooth, this.round);
        this.parameters .fill();

        this            ._draw(graphics);
        return          this;
    }

    public SdfGraphics stroke(@NotNull GuiGraphicsExtractor graphics, float width) {
        this.parameters .shared(this.smooth, this.round);
        this.parameters .stroke(width);

        this            ._draw(graphics);
        return          this;
    }

    public SdfGraphics light(@NotNull GuiGraphicsExtractor graphics, float radius) {
        var last        = this.smooth;
        this.smooth     = radius;

        this.parameters .shared(4.605f / this.smooth, this.round);
        this.parameters .light();

        this            ._draw(graphics);
        this.smooth     = last;
        return          this;
    }

    public SdfGraphics flush() {
        this.x          = 0;
        this.y          = 0;
        this.w          = 0;
        this.h          = 0;
        this.color      = 0;
        this.smooth     = 0;
        this.round      = 0;
        this.rotation   = 0;
        this.center     = false;
        this.index      = 0;
        return          this;
    }

    private void _draw(@NotNull GuiGraphicsExtractor graphics) {
        var pose        = new Matrix3x2f(graphics.pose());
        var ex          = (this.round + this.smooth) * 2.0f;
        var width       = this.w + ex;
        var height      = this.h + ex;

        if (this.center) {
            pose    .translate(this.x, this.y);
        } else {
            pose    .translate(this.x + this.w * 0.5f, this.y + this.h * 0.5f);
        }

        var x0          = -0.5f;
        var y0          = -0.5f;
        var x1          = +0.5f;
        var y1          = +0.5f;

        pose            .rotate(Mth.DEG_TO_RAD * this.rotation)
                        .scale(width, height);

        this.parameters .getRect()
                        .set(
                                x0,
                                y0,
                                width,
                                height
                        );

        var offset      = this.index * SDF_PARAMETER_SIZE;
        var slice       = this.ubo.slice(offset, SDF_PARAMETER_SIZE);
        var state       = new RenderState(
                        pose,
                        x0, y0,
                        x1, y1,
                        this.color,
                        this.index,
                        this.ubo.slice(),
                        null
        );

        this.parameters .upload(this.encoder, slice);

        graphics        .submitGuiElementRenderState(state);

        this.index      ++;
    }

    public record RenderState(
            Matrix3x2f pose,
            float x0,
            float y0,
            float x1,
            float y1,
            int color,
            int index,
            GpuBufferSlice sdfParametersUbo,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements LibQuadGuiElementRenderState {

        private RenderState(
                Matrix3x2f pose,
                float x0,
                float y0,
                float x1,
                float y1,
                int color,
                int index,
                GpuBufferSlice sdfParametersUbo,
                @Nullable ScreenRectangle scissorArea
        ) {
            this(
                    pose,
                    x0,
                    y0,
                    x1,
                    y1,
                    color,
                    index,
                    sdfParametersUbo, scissorArea,
                    LibGuiElementRenderState.getBounds(pose, x0, y0, x1, y1, scissorArea)
            );
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            consumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setUv(0, 0).setUv1(this.index(), 0).setColor(this.color());
            consumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setUv(0, 1).setUv1(this.index(), 0).setColor(this.color());
            consumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setUv(1, 1).setUv1(this.index(), 0).setColor(this.color());
            consumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setUv(1, 0).setUv1(this.index(), 0).setColor(this.color());
        }

        @Override
        public @NonNull RenderPipeline pipeline() {
            return ALRPipelines.SDF_GRAPHICS;
        }

        @Override
        public Map<String, GpuBufferSlice> bufferSlices() {
            return Map.of("SDFParameters", this.sdfParametersUbo());
        }
    }
}
