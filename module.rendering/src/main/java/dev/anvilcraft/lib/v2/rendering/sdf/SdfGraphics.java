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
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
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
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@EventBusSubscriber(modid = AnvilLibRendering.MODID, value = Dist.CLIENT)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SdfGraphics {
    private static final int            MAX_SDF_AMOUNT          = 256;
    private static final int            MAX_SHARED_SDF_AMOUNT   = 64;
    private static final long           SDF_PARAMETER_SIZE      = SdfParameters.DEFINITION.size(BufferLayout.STD140);
    @Getter
    public static final SdfGraphics     instance                = new SdfGraphics(new SdfParameters());

    private static boolean              debug                   = false;

    private final SdfParameters[]       shared                  = new SdfParameters[MAX_SHARED_SDF_AMOUNT];

    private final SdfParameters         parameters;

    private int                         shareCursor             = 0;

    private float                       x, y, rotation;
    private int                         color                   = -1;
    private boolean                     centred                 = false;

    public SdfGraphics box(float x, float y, float width, float height) {
        this.parameters .getRect()
                        .set(0, 0, width, height);
        this.parameters .box(width, height);
        this.x          = x;
        this.y          = y;

        return          this;
    }

    public SdfGraphics circle(float x, float y, float radius) {
        this.parameters .getRect()
                        .set(0, 0, radius * 2, radius * 2);
        this.parameters .circle(radius);
        this.x          = x;
        this.y          = y;

        return          this;
    }

    public SdfGraphics arc(float x, float y, float sweep, float radius, float width) {
        var scale       = radius * 2 + width;

        this.parameters .getRect()
                        .set(0, 0, scale, scale);
        this.parameters .arc(sweep, radius, width);
        this.x          = x;
        this.y          = y;

        return          this;
    }

    public SdfGraphics sector(float x, float y, float sweep, float radius, float width) {
        this.parameters .getRect()
                        .set(0, 0, radius * 2, radius * 2);
        this.parameters .sector(sweep, radius, width);
        this.x          = x;
        this.y          = y;

        return          this;
    }

    public SdfGraphics pie(float x, float y, float sweep, float radius) {
        this.parameters .getRect()
                        .set(0, 0, radius * 2, radius * 2);
        this.parameters .pie(sweep, radius);
        this.x          = x;
        this.y          = y;

        return          this;
    }

    public SdfGraphics capsule(
            float x, float y,
            float topRadius, float bottomRadius,
            float height
    ) {
        var width       = Math.max(topRadius, bottomRadius) * 2;

        this.parameters .getRect()
                        .set(0, 0, width, height + (topRadius + bottomRadius) * 3);
        this.parameters .capsule(topRadius, bottomRadius, height);
        this.x          = x;
        this.y          = y;

        return          this;
    }

    public SdfGraphics egg(
            float x, float y,
            float topRadius, float bottomRadius,
            float height
    ) {
        var width       = Math.max(topRadius, bottomRadius) * 2;

        this.parameters .getRect()
                        .set(0, 0, width, height + (topRadius + bottomRadius) * 2);
        this.parameters .egg(topRadius, bottomRadius, height);
        this.x          = x;
        this.y          = y;

        return          this;
    }

    public SdfGraphics segment(
            float x0, float y0,
            float x1, float y1
    ) {
        var left        = Math.min(x0, x1);
        var top         = Math.min(y0, y1);
        var width       = Math.abs(x1 - x0);
        var height      = Math.abs(y1 - y0);

        var halfWidth   = (x1 - x0) * 0.5f;
        var halfHeight  = (y1 - y0) * 0.5f;

        this.parameters .getRect()
                        .set(left + width / 2, top + height / 2, width, height);
        this.parameters .segment( -halfWidth, -halfHeight, +halfWidth, +halfHeight);
        this.x          = left + width / 2;
        this.y          = top + height / 2;

        return          this;
    }

    public SdfGraphics triangleEquilateral(float x, float y, float radius) {
        var actual      = radius * (1.0f / 1.2f);

        this.parameters .getRect()
                        .set(0, 0, radius * 2, radius * 2);
        this.parameters .triangleEquilateral(actual);
        this.x          = x;
        this.y          = y;

        return          this;
    }

    public SdfGraphics triangleIsosceles(float x, float y, float width, float height) {
        final var factor = 1.0f / 1.2f;
        this.parameters .getRect()
                        .set(0, 0, width * 2.0f, height);
        this.parameters .triangleIsosceles(width * factor, height * factor);
        this.x          = x;
        this.y          = y;

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
        this.rotation   = degrees;
        return          this;
    }

    public SdfGraphics center(boolean center) {
        this.centred    = center;
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

    public SdfGraphics draw(
            @NotNull GuiGraphicsExtractor   graphics
    ) {
        _draw(
                graphics, 
                this.parameters, 
                this.x, 
                this.y,
                this.rotation,
                this.color,
                this.centred
        );
        return          this;
    }

    public SdfGraphics draw(
            @NotNull GuiGraphicsExtractor   graphics,
            @NotNull SdfParameters          parameters,
                     float                  x,
                     float                  y
    ) {

        if (parameters.isShared()) {
            _draw(
                    graphics, 
                    parameters, 
                    x, 
                    y,
                    this.rotation,
                    this.color,
                    this.centred
            );
        }

        return this;
    }

    public SdfGraphics reset() {
        this.parameters .reset();
        this.x          = 0;
        this.y          = 0;
        this.rotation   = 0;
        this.color      = -1;
        this.centred    = false;
        return          this;
    }

    public boolean collide(float x, float y, float threshold) {
        return this.collide(
                this.parameters,
                x,
                y,
                this.x,
                this.y,
                threshold
        );
    }

    public boolean collide(
            @NotNull SdfParameters          parameters,
                     float                  pointX,
                     float                  pointY,
                     float                  x,
                     float                  y,
                     float                  threshold
    ) {
        return Sdf2d    .sd(
                            parameters,
                            pointX,
                            pointY,
                            x,
                            y,
                            this.rotation,
                            this.centred
                        ) < threshold;
    }

    public @NonNull SdfParameters cache() {
        return this.parameters.duplicate();
    }

    public @NonNull SdfParameters share() {
        final var cache = this.cache();
        this.share(cache);
        return cache;
    }

    public void share(@NotNull SdfParameters cache) {
        final var cursor = this.shareCursor;
        if (cursor == MAX_SHARED_SDF_AMOUNT) {
            return;
        }

        this.shared[cursor]     = cache;
        cache.uboIndex          = MAX_SDF_AMOUNT - cursor - 1;
        cache.uploaded          = false;

        do {
            this.shareCursor++;
        } while (this.shareCursor < MAX_SHARED_SDF_AMOUNT && this.shared[this.shareCursor] != null);
    }

    public void unshare(@NotNull SdfParameters cache) {
        var cursor = 0;
        // find index
        for (; cursor < MAX_SHARED_SDF_AMOUNT; cursor++) {
            if (this.shared[cursor] == cache) {
                cache.uploaded = false;
                cache.uboIndex = -1;
                break;
            }
        }

        if (cursor == MAX_SHARED_SDF_AMOUNT) {
            return;
        }

        this.shared[cursor] = null;
        if (cursor < this.shareCursor) {
            this.shareCursor = cursor;
        }
    }

    public static void flush() {
        instance.parameters .reset();
        SdfGraphics.index   = 0;
    }

    private static  CommandEncoder  encoder;
    private static  GpuBuffer       ubo;

    private static  int             index;

    @ApiStatus.Internal
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

    public static void debug(boolean enable) {
        SdfGraphics.debug = enable;
    }

    private static void _draw(
            @NotNull GuiGraphicsExtractor   graphics,
            @NotNull SdfParameters          parameters,
                     float                  x,
                     float                  y,
                     float                  rotation,
                     int                    color,
                     boolean                centred
    ) {
        var round           = parameters.getRound();
        var smooth          = parameters.getSmooth();
        var stroke          = parameters.getStroke();

        var rect            = parameters.getRect();
        var z               = rect.z;
        var w               = rect.w;

        var pose            = new Matrix3x2f(graphics.pose());
        var ex              = (round + smooth + stroke) * 2.0f;
        var width           = rect.z + ex;
        var height          = rect.w + ex;
        final var hw        = width * 0.5f;
        final var hh        = height * 0.5f;

        final var radian    = rotation * Mth.DEG_TO_RAD;
        final var cos       = Mth.cos(radian);
        final var sin       = Mth.sin(radian);

        float cx;
        float cy;
        if (centred) {
            pose            .translate(x, y);

            cx              = x;
            cy              = y;
        } else {
            pose            .translate(
                            x + hw,
                            y + hh
                            );

            cx              = x + hw - hw * cos + hh * sin;
            cy              = y + hh - hw * sin - hh * cos;
        }

        if (rotation != 0.0f) {
            pose            .rotate(Mth.DEG_TO_RAD * rotation);
        }

        if (!centred) {
            pose            .translate(-hw, -hh);
        }

        pose                .scale(width, height);

        rect.z              = width;
        rect.w              = height;

        var extX            = Mth.abs(hw * cos) + Mth.abs(hh * sin) + 1.0f;
        var extY            = Mth.abs(hw * sin) + Mth.abs(hh * cos) + 1.0f;

        var x0              = cx - extX;
        var x1              = cx + extX;
        var y0              = cy - extY;
        var y1              = cy + extY;

        var shared          = parameters.isShared();
        var idx             = shared ? parameters.uboIndex : index;
        var state           = new RenderState(
                                pose,
                                color,
                                idx,
                                ubo.slice(),
                                graphics.peekScissorStack(),
                                LibGuiElementRenderState.getBounds(
                                        new Matrix3x2f(graphics.pose()),
                                        x0, y0,
                                        x1, y1,
                                        graphics.peekScissorStack()
                                )
                            );

        if (debug) {
            graphics        .outline(
                                    (int) x0, (int) y0,
                                    (int) (x1 - x0), (int) (y1 - y0),
                                    0xFF0000FF
                            );
        }

        if (!shared || !parameters.uploaded) {
            var offset      = idx * SDF_PARAMETER_SIZE;
            var slice       = ubo.slice(offset, SDF_PARAMETER_SIZE);
            parameters      .upload(encoder, slice);
            parameters      .uploaded = shared;
        }

        graphics            .submitGuiElementRenderState(state);
        rect.z              = z;
        rect.w              = w;

        if (!shared) {
            SdfGraphics     .index++;
        }
    }

    private record RenderState(
            Matrix3x2f                  pose,
            int                         color,
            int                         index,
            GpuBufferSlice              sdfParametersUbo,
            @Nullable ScreenRectangle   scissorArea,
            @Nullable ScreenRectangle   bounds
    ) implements LibGuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {
            consumer.addVertexWith2DPose(this.pose(), -0.5f, -0.5f).setUv(0, 0).setUv1(this.index(), 0).setColor(this.color());
            consumer.addVertexWith2DPose(this.pose(), -0.5f, +0.5f).setUv(0, 1).setUv1(this.index(), 0).setColor(this.color());
            consumer.addVertexWith2DPose(this.pose(), +0.5f, +0.5f).setUv(1, 1).setUv1(this.index(), 0).setColor(this.color());
            consumer.addVertexWith2DPose(this.pose(), +0.5f, -0.5f).setUv(1, 0).setUv1(this.index(), 0).setColor(this.color());
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
