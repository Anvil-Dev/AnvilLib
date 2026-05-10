package dev.anvilcraft.lib.v2.rendering.sdf;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.ALRPipelines;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import dev.anvilcraft.lib.v2.rendering.state.LibGuiElementRenderState;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@EventBusSubscriber(modid = AnvilLibRendering.MODID, value = Dist.CLIENT)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SdfGraphics {
    private static final int            MAX_SDF_AMOUNT          = 256;
    private static final long           SDF_PARAMETER_SIZE      = SdfParameters.DEFINITION.size();
    @Getter
    public static final SdfGraphics     instance                = new SdfGraphics(new SdfParameters());

    private final SdfParameters parameters;

    public SdfGraphics box(float x, float y, float width, float height) {
        this.parameters .getRect()
                        .set(x, y, width, height);
        this.parameters .box(width, height);

        return          this;
    }

    public SdfGraphics circle(float x, float y, float radius) {
        this.parameters .getRect()
                        .set(x, y, radius * 2, radius * 2);
        this.parameters .circle(radius);

        return          this;
    }

    public SdfGraphics arc(float x, float y, float sweep, float radius, float width) {
        var scale       = radius * 2 + width;

        this.parameters .getRect()
                        .set(x, y, scale, scale);
        this.parameters .arc(sweep, radius, width);

        return          this;
    }

    public SdfGraphics sector(float x, float y, float sweep, float radius, float width) {
        this.parameters .getRect()
                        .set(x, y, radius * 2, radius * 2);
        this.parameters .sector(sweep, radius, width);

        return          this;
    }

    public SdfGraphics pie(float x, float y, float sweep, float radius) {
        this.parameters .getRect()
                        .set(x, y, radius * 2, radius * 2);
        this.parameters .pie(sweep, radius);

        return          this;
    }

    public SdfGraphics capsule(
            float x, float y,
            float topRadius, float bottomRadius,
            float height
    ) {
        var width       = Math.max(topRadius, bottomRadius) * 2;

        this.parameters .getRect()
                        .set(x, y, width, height + (topRadius + bottomRadius) * 3);
        this.parameters .capsule(topRadius, bottomRadius, height);

        return          this;
    }

    public SdfGraphics egg(
            float x, float y,
            float topRadius, float bottomRadius,
            float height
    ) {
        var width       = Math.max(topRadius, bottomRadius) * 2;

        this.parameters .getRect()
                        .set(x, y, width, height + (topRadius + bottomRadius) * 2);
        this.parameters .egg(topRadius, bottomRadius, height);

        return          this;
    }

    public SdfGraphics color(int color) {
        this.parameters .color(color);
        return          this;
    }

    public SdfGraphics color(float red, float green, float blue, float alpha) {
        this.parameters .color(ARGB.colorFromFloat(red, green, blue, alpha));
        return          this;
    }

    public SdfGraphics color(int red, int green, int blue, int alpha) {
        this.parameters .color(ARGB.color(alpha, red, green, blue));
        return          this;
    }

    public SdfGraphics smooth(float radius) {
        var v           = Math.max(0.0f, radius);
        this.parameters .smooth(v);
        return          this;
    }

    public SdfGraphics round(float radius) {
        var v           = Math.max(0.0f, radius);
        this.parameters .round(v);
        return          this;
    }

    public SdfGraphics stroke(float width) {
        var v           = Math.max(0.0f, width * 0.5f);
        this.parameters .stroke(v);
        this.parameters .onion(width > 0.0f);
        return          this;
    }

    public SdfGraphics rotate(float degrees) {
        this.parameters .rotate(Mth.wrapDegrees(degrees));
        return          this;
    }

    public SdfGraphics center(boolean center) {
        this.parameters .center(center);
        return          this;
    }

    public SdfGraphics onion(boolean onion) {
        this.parameters .onion(onion);
        return          this;
    }

    public SdfGraphics fill() {
        this.parameters .fill();
        return          this;
    }

    public SdfGraphics light(float radius) {
        this.parameters .light(radius);
        return          this;
    }

    public SdfGraphics draw(@NotNull GuiGraphicsExtractor graphics) {
        _draw(graphics, this.parameters);
        return          this;
    }

    public SdfGraphics reset() {
        this.parameters .reset();
        return          this;
    }

    public boolean collide(float x, float y, float threshold) {
        return Sdf2d    .sd(this.parameters, x, y) < threshold;
    }

    public SdfGraphics cache() {
        return new SdfGraphics(this.parameters.duplicate());
    }

    public static void flush() {
        instance.parameters .reset();
        SdfGraphics.index   = 0;
    }

    private static  CommandEncoder  encoder;
    private static  GpuBuffer       ubo;

    private static  int             index;

    @SubscribeEvent
    public static void init(ConfigureMainRenderTargetEvent event) {
        GpuDevice device    = RenderSystem.getDevice();
        encoder             = device.createCommandEncoder();
        ubo                 = device.createBuffer(
                () -> "SDF Parameters",
                GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
                SDF_PARAMETER_SIZE * MAX_SDF_AMOUNT
        );
    }

    private static void _draw(
            @NotNull GuiGraphicsExtractor graphics,
            @NotNull SdfParameters parameters
    ) {
        var round       = parameters.getRound();
        var smooth      = parameters.getSmooth();
        var stroke      = parameters.getStroke();

        var rect        = parameters.getRect();
        var z           = rect.z;
        var w           = rect.w;

        var pose        = new Matrix3x2f(graphics.pose());
        var ex          = (round + smooth + stroke) * 2.0f;
        var width       = rect.z + ex;
        var height      = rect.w + ex;

        if (parameters.isCenter()) {
            pose        .translate(rect.x, rect.y);
        } else {
            pose        .translate(
                        rect.x + width * 0.5f,
                        rect.y + height * 0.5f
            );
        }

        var x0          = -0.5f;
        var y0          = -0.5f;
        var x1          = +0.5f;
        var y1          = +0.5f;

        pose            .rotate(Mth.DEG_TO_RAD * parameters.getRotation())
                        .scale(width, height);

        rect.z          = width;
        rect.w          = height;

        var offset      = index * SDF_PARAMETER_SIZE;
        var slice       = ubo.slice(offset, SDF_PARAMETER_SIZE);
        var state       = new RenderState(
                        pose,
                        x0, y0,
                        x1, y1,
                        parameters.getColor(),
                        index,
                        ubo.slice(),
                        null
        );

        parameters      .upload(encoder, slice);

        graphics        .submitGuiElementRenderState(state);

        rect.z          = z;
        rect.w          = w;

        SdfGraphics.index++;
    }

    private static boolean _collide(
            @NotNull SdfParameters parameters,
            float x,
            float y
    ) {
        return false;
    }

    private record RenderState(
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
    ) implements LibGuiElementRenderState {

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
        public @NonNull TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public Map<String, GpuBufferSlice> bufferSlices() {
            return Map.of("SDFParameters", this.sdfParametersUbo());
        }
    }

}
