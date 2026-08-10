package dev.anvilcraft.lib.v2.rendering.sdf;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.postprocess.GlUniformBuffer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Draws 2D shapes (circle/rect/rounded rect/...) on top of a {@link GuiGraphics} using the nine SDF
 * distance functions declared in {@code sdf_graphics.fsh}.
 * <p>
 * Ported from 26.1 (see #P7c): the original submitted {@code LibGuiElementRenderState}s through the
 * 26.1 GUI render pipeline (with {@code Matrix3x2f} poses and a {@code GpuDevice} UBO); on 1.21.1 the
 * shapes are appended as 4-vertex quads into the {@link GuiGraphics} {@code BufferSource} using a custom
 * {@link RenderType} bound to the {@code sdf_graphics} {@link ShaderInstance} (registered through
 * {@code RegisterShadersEvent}, see {@link SdfShaders}). Per-shape parameters are uploaded into a
 * self-managed {@code layout(std140) uniform SDFParameters} UBO (one slot per quad, referenced through
 * {@code UV1.x}); the UBO is bound through {@code glBindBufferRange} right before the batch is drawn.
 * <p>
 * The {@code mixins/GuiRendererMixin} SDF injection of the 26.1 branch is <b>not</b> ported: its target
 * class ({@code GuiRenderer}) does not exist on 1.21.1. {@link #flush()} is instead invoked automatically
 * at the start of every frame (see {@code AnvilLibRendering}).
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SdfGraphics {
    private static final int MAX_SDF_AMOUNT = 256;
    private static final int MAX_SHARED_SDF_AMOUNT = 64;
    private static final int SDF_PARAMETER_SIZE = SdfParameters.DEFINITION.size(BufferLayout.STD140);
    /**
     * GL uniform buffer binding point used by the SDF parameters block. The post-processing pipeline
     * ({@link GlUniformBuffer}) uses points 0 and 1, so 2 is free for the SDF UBO.
     */
    private static final int UBO_BINDING_POINT = 2;

    @Getter
    public static final SdfGraphics instance = new SdfGraphics(new SdfParameters());

    private static boolean debug = false;

    private final SdfParameters[] shared = new SdfParameters[MAX_SHARED_SDF_AMOUNT];

    private final SdfParameters parameters;

    /** Self-managed {@code SDFParameters} UBO (all 256 slots). Created lazily on the first draw. */
    private static int uboId = -1;
    private static final ByteBuffer SLOT_BUFFER = ByteBuffer.allocateDirect(SDF_PARAMETER_SIZE).order(ByteOrder.nativeOrder());

    private int shareCursor = 0;

    private float x, y, rotation;
    private int color = -1;
    private boolean centred = false;

    private static int index;

    private static final RenderType SDF_RENDER_TYPE = RenderType.create(
        "anvillib_rendering:sdf_graphics",
        DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(SdfGraphics::bindAndGetShader))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false)
    );

    public SdfGraphics box(float x, float y, float width, float height) {
        this.parameters.getRect().set(0, 0, width, height);
        this.parameters.box(width, height);
        this.x = x;
        this.y = y;

        return this;
    }

    public SdfGraphics circle(float x, float y, float radius) {
        this.parameters.getRect().set(0, 0, radius * 2, radius * 2);
        this.parameters.circle(radius);
        this.x = x;
        this.y = y;

        return this;
    }

    public SdfGraphics arc(float x, float y, float sweep, float radius, float width) {
        var scale = radius * 2 + width;

        this.parameters.getRect().set(0, 0, scale, scale);
        this.parameters.arc(sweep, radius, width);
        this.x = x;
        this.y = y;

        return this;
    }

    public SdfGraphics sector(float x, float y, float sweep, float radius, float width) {
        this.parameters.getRect().set(0, 0, radius * 2, radius * 2);
        this.parameters.sector(sweep, radius, width);
        this.x = x;
        this.y = y;

        return this;
    }

    public SdfGraphics pie(float x, float y, float sweep, float radius) {
        this.parameters.getRect().set(0, 0, radius * 2, radius * 2);
        this.parameters.pie(sweep, radius);
        this.x = x;
        this.y = y;

        return this;
    }

    public SdfGraphics capsule(
        float x, float y,
        float topRadius, float bottomRadius,
        float height
    ) {
        var width = Math.max(topRadius, bottomRadius) * 2;

        this.parameters.getRect().set(0, 0, width, height + (topRadius + bottomRadius) * 3);
        this.parameters.capsule(topRadius, bottomRadius, height);
        this.x = x;
        this.y = y;

        return this;
    }

    public SdfGraphics egg(
        float x, float y,
        float topRadius, float bottomRadius,
        float height
    ) {
        var width = Math.max(topRadius, bottomRadius) * 2;

        this.parameters.getRect().set(0, 0, width, height + (topRadius + bottomRadius) * 2);
        this.parameters.egg(topRadius, bottomRadius, height);
        this.x = x;
        this.y = y;

        return this;
    }

    public SdfGraphics segment(
        float x0, float y0,
        float x1, float y1
    ) {
        var left = Math.min(x0, x1);
        var top = Math.min(y0, y1);
        var width = Math.abs(x1 - x0);
        var height = Math.abs(y1 - y0);

        var halfWidth = (x1 - x0) * 0.5f;
        var halfHeight = (y1 - y0) * 0.5f;

        this.parameters.getRect().set(left + width / 2, top + height / 2, width, height);
        this.parameters.segment(-halfWidth, -halfHeight, +halfWidth, +halfHeight);
        this.x = left + width / 2;
        this.y = top + height / 2;

        return this;
    }

    public SdfGraphics triangleEquilateral(float x, float y, float radius) {
        var actual = radius * (1.0f / 1.2f);

        this.parameters.getRect().set(0, 0, radius * 2, radius * 2);
        this.parameters.triangleEquilateral(actual);
        this.x = x;
        this.y = y;

        return this;
    }

    public SdfGraphics triangleIsosceles(float x, float y, float width, float height) {
        final var factor = 1.0f / 1.2f;
        this.parameters.getRect().set(0, 0, width * 2.0f, height);
        this.parameters.triangleIsosceles(width * factor, height * factor);
        this.x = x;
        this.y = y;

        return this;
    }

    public SdfGraphics color(int color) {
        this.color = color;
        return this;
    }

    public SdfGraphics color(float red, float green, float blue, float alpha) {
        this.color = FastColor.ARGB32.color(
            (int) (alpha * 255.0F),
            (int) (red * 255.0F),
            (int) (green * 255.0F),
            (int) (blue * 255.0F)
        );
        return this;
    }

    public SdfGraphics color(int red, int green, int blue, int alpha) {
        this.color = FastColor.ARGB32.color(alpha, red, green, blue);
        return this;
    }

    public SdfGraphics smooth(float radius) {
        var v = Math.max(0.0f, radius);
        this.parameters.smooth(v);
        return this;
    }

    public SdfGraphics round(float radius) {
        var v = Math.max(0.0f, radius);
        this.parameters.round(v);
        return this;
    }

    public SdfGraphics stroke(float width) {
        var v = Math.max(0.0f, width * 0.5f);
        this.parameters.stroke(v);
        this.parameters.onion(width > 0.0f);
        return this;
    }

    public SdfGraphics rotate(float degrees) {
        this.rotation = degrees;
        return this;
    }

    public SdfGraphics center(boolean center) {
        this.centred = center;
        return this;
    }

    public SdfGraphics onion(boolean onion) {
        this.parameters.onion(onion);
        return this;
    }

    public SdfGraphics fill() {
        this.parameters.fill();
        return this;
    }

    public SdfGraphics light(float radius) {
        this.parameters.light(radius);
        return this;
    }

    public SdfGraphics draw(
        @NotNull GuiGraphics graphics
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
        return this;
    }

    public SdfGraphics draw(
        @NotNull GuiGraphics graphics,
        @NotNull SdfParameters parameters,
        float x,
        float y
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
        this.parameters.reset();
        this.x = 0;
        this.y = 0;
        this.rotation = 0;
        this.color = -1;
        this.centred = false;
        return this;
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
        @NotNull SdfParameters parameters,
        float pointX,
        float pointY,
        float x,
        float y,
        float threshold
    ) {
        return Sdf2d.sd(
            parameters,
            pointX,
            pointY,
            x,
            y,
            this.rotation,
            this.centred
        ) < threshold;
    }

    public @NotNull SdfParameters cache() {
        return this.parameters.duplicate();
    }

    public @NotNull SdfParameters share() {
        final var cache = this.cache();
        this.share(cache);
        return cache;
    }

    public void share(@NotNull SdfParameters cache) {
        final var cursor = this.shareCursor;
        if (cursor == MAX_SHARED_SDF_AMOUNT) {
            return;
        }

        this.shared[cursor] = cache;
        cache.uboIndex = MAX_SDF_AMOUNT - cursor - 1;
        cache.uploaded = false;

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

    /**
     * Resets the per-frame SDF slot cursor (and the singleton's parameters). Called automatically at the
     * start of every frame; kept public for manual use inside screens.
     */
    public static void flush() {
        instance.parameters.reset();
        SdfGraphics.index = 0;
    }

    public static void debug(boolean enable) {
        SdfGraphics.debug = enable;
    }

    /**
     * Lazily creates the SDF parameters UBO and (re)binds it to the SDF uniform block of the current
     * shader program. This runs as the {@code ShaderStateShard} supplier, i.e. right before the SDF
     * batch is drawn, so the binding is guaranteed to be current at draw time even if another pipeline
     * touched the binding point in between.
     */
    private static ShaderInstance bindAndGetShader() {
        ShaderInstance shader = SdfShaders.getSdfGraphicsShader();
        if (shader == null) {
            return null;
        }
        if (uboId == -1) {
            uboId = GL31.glGenBuffers();
            GL31.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
            GL31.glBufferData(GL31.GL_UNIFORM_BUFFER, (long) SDF_PARAMETER_SIZE * MAX_SDF_AMOUNT, GL31.GL_DYNAMIC_DRAW);
        }
        int programId = shader.getId();
        int blockIndex = GL31.glGetUniformBlockIndex(programId, "SDFParameters");
        if (blockIndex != GL31.GL_INVALID_INDEX) {
            GL31.glUniformBlockBinding(programId, blockIndex, UBO_BINDING_POINT);
        }
        GL31.glBindBufferRange(
            GL31.GL_UNIFORM_BUFFER,
            UBO_BINDING_POINT,
            uboId,
            0L,
            (long) SDF_PARAMETER_SIZE * MAX_SDF_AMOUNT
        );
        return shader;
    }

    private static void _draw(
        @NotNull GuiGraphics graphics,
        @NotNull SdfParameters parameters,
        float x,
        float y,
        float rotation,
        int color,
        boolean centred
    ) {
        var round = parameters.getRound();
        var smooth = parameters.getSmooth();
        var stroke = parameters.getStroke();

        var rect = parameters.getRect();
        var z = rect.z;
        var w = rect.w;

        var pose = graphics.pose();
        var ex = (round + smooth + stroke) * 2.0f;
        var width = rect.z + ex;
        var height = rect.w + ex;
        final var hw = width * 0.5f;
        final var hh = height * 0.5f;

        final var radian = rotation * Mth.DEG_TO_RAD;
        final var cos = Mth.cos(radian);
        final var sin = Mth.sin(radian);

        float cx;
        float cy;
        pose.pushPose();
        if (centred) {
            pose.translate(x, y, 0.0f);

            cx = x;
            cy = y;
        } else {
            pose.translate(
                x + hw,
                y + hh,
                0.0f
            );

            cx = x + hw - hw * cos + hh * sin;
            cy = y + hh - hw * sin - hh * cos;
        }

        if (rotation != 0.0f) {
            pose.mulPose(Axis.ZP.rotationDegrees(rotation));
        }

        if (!centred) {
            pose.translate(-hw, -hh, 0.0f);
        }

        pose.scale(width, height, 1.0f);

        rect.z = width;
        rect.w = height;

        var extX = Mth.abs(hw * cos) + Mth.abs(hh * sin) + 1.0f;
        var extY = Mth.abs(hw * sin) + Mth.abs(hh * cos) + 1.0f;

        var x0 = cx - extX;
        var x1 = cx + extX;
        var y0 = cy - extY;
        var y1 = cy + extY;

        var shared = parameters.isShared();
        var idx = shared ? parameters.uboIndex : index;

        // If the sdf_graphics shader failed to register, don't append quads: a RenderType whose
        // shader supplier returns null would NPE inside BufferUploader at flush time.
        if (SdfShaders.getSdfGraphicsShader() == null) {
            pose.popPose();
            rect.z = z;
            rect.w = w;
            return;
        }

        if (debug) {
            graphics.renderOutline(
                (int) x0, (int) y0,
                (int) (x1 - x0), (int) (y1 - y0),
                0xFF0000FF
            );
        }

        if (idx >= MAX_SDF_AMOUNT) {
            pose.popPose();
            rect.z = z;
            rect.w = w;
            return;
        }

        if (!shared || !parameters.uploaded) {
            upload(parameters, idx);
            parameters.uploaded = shared;
        }

        var poseEntry = pose.last();
        VertexConsumer consumer = graphics.bufferSource().getBuffer(SDF_RENDER_TYPE);
        consumer.addVertex(poseEntry, -0.5f, -0.5f, 0.0f).setUv(0, 0).setUv1(idx, 0).setColor(color);
        consumer.addVertex(poseEntry, -0.5f, +0.5f, 0.0f).setUv(0, 1).setUv1(idx, 0).setColor(color);
        consumer.addVertex(poseEntry, +0.5f, +0.5f, 0.0f).setUv(1, 1).setUv1(idx, 0).setColor(color);
        consumer.addVertex(poseEntry, +0.5f, -0.5f, 0.0f).setUv(1, 0).setUv1(idx, 0).setColor(color);

        pose.popPose();
        rect.z = z;
        rect.w = w;

        if (!shared) {
            SdfGraphics.index++;
        }
    }

    private static void upload(@NotNull SdfParameters parameters, int slot) {
        if (uboId == -1) {
            return;
        }
        SLOT_BUFFER.clear();
        parameters.write(SLOT_BUFFER);
        GL31.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboId);
        GL31.glBufferSubData(
            GL31.GL_UNIFORM_BUFFER,
            (long) slot * SDF_PARAMETER_SIZE,
            SLOT_BUFFER
        );
        GL31.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }
}
