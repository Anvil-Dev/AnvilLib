package dev.anvilcraft.lib.v2.wheel.client.gui.component;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.init.LibShaders;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

@Slf4j
@SuppressWarnings(
    {
        "UnusedReturnValue",
        "unused"
    }
)
public class WheelWidget extends AbstractWidget {
    public static final int IGNORE_CURSOR_MOVE_LENGTH = 15;
    public static final int DEFAULT_SELECTION_EFFECT_COLOR = 0xFFFABC02;
    private static final float SELECTION_DOT_DIAMETER_RATIO = 0.9f;
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final float ANGLE_AA_RAD = 0.06f;
    private static final Vector2f ROTATION_START = new Vector2f(0, 1);
    // 中心区域与扇区之间的白色不透明分隔圆环：半径在中心区基础上外扩的距离与线宽
    // （dev/26.1 的 SdfGraphics.stroke 实际厚度为入参一半）
    private static final float SEPARATOR_RING_GAP = 4f;
    private static final float SEPARATOR_RING_THICKNESS = 2f;
    // 高亮扇区内缘相对中心区的内缩量（贴合分隔圆环外侧）
    private static final float SECTION_INNER_INSET = 8f;
    // 未停住时高亮扇区外缘相对盘面外边缘的内缩量；停住后内外缘同步外扩 SETTLE_EXPAND 距离
    private static final float SECTION_OUTER_INSET = 2f;
    // 鼠标停住后高亮扇区内外缘同步向外扩张的距离与动画参数
    private static final float SETTLE_EXPAND = 2f;
    private static final long SETTLE_DELAY_MS = 500L;
    private static final long SETTLE_ANIM_MS = 200L;
    // 指向鼠标方向的尖括号箭头（贴在分隔圆环内侧）尺寸与线宽
    private static final float HOVER_CHEVRON_SIZE = 9f;
    private static final float HOVER_CHEVRON_THICKNESS = 4f;
    private static final float HOVER_CHEVRON_RING_GAP = 4f;
    // 中心悬停名缩放倍率
    private static final float CENTER_TITLE_SCALE = 1.4f;
    // 翻页箭头（尖括号）尺寸与线宽
    private static final float CHEVRON_SIZE = 8f;
    private static final float CHEVRON_THICKNESS = 3f;
    // 毛玻璃盘面叠加色：白 * 40% 透明度，混合在深色盘面上
    private static final int FROSTED_TINT = 0x66FFFFFF;
    // 绘制深度分层：盘面/毛玻璃 < 分隔圆环 < 高亮 < 图标
    private static final float DISC_Z = 50f;
    private static final float RING_Z = 60f;
    private static final float SELECTION_Z = 80f;

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Vector2f centerPos;
    private final float ringInnerRadius;
    private final float ringOuterRadius;
    private final int delay; // ms
    private final int animationMs; // ms
    private final int closingAnimationMs; // ms
    private final int ringColor;
    private int selectionEffectColor;
    private final float selectionAnimationSpeedFactor;
    private final int textColor;
    private final float textScale;
    private final List<WheelSection> sections = new ArrayList<>();

    private long displayTime = System.currentTimeMillis();
    private float currentAngle = 0;
    @Getter
    private int currentSectionIndex = -1;
    // 环扇高亮当前渲染角度（弧度，轮盘坐标系），每帧向目标扇区平滑逼近
    private float selectionAngleRad = 0;
    // 最近一次选中扇区变化的时间，用于“鼠标停住”检测
    private long selectionChangeTime = System.currentTimeMillis();
    // 鼠标相对轮盘中心的方向角（弧度，与 WheelSection.angle 同坐标系），在死区内时为 null
    @Nullable
    private Float mouseAngleRad;
    private Vector2f selectionEffectPos;
    private boolean animationStarted = false;
    @Getter
    @Setter
    private boolean closingAnimationStarted = false;
    private final int deadZone;
    private WheelSelectionEffect selectionEffect = WheelSelectionEffect.DOT;
    @Nullable
    private WheelFrostedBackground frostedBackground;
    private boolean hasPreviousPage;
    private boolean hasNextPage;

    public WheelWidget(
        int x,
        int y,
        int width,
        int height,
        float ringInnerRadius,
        float ringOuterRadius,
        float textScale,
        List<RawSection> sections,
        int deadZone
    ) {
        this(x, y, width, height, Component.empty(), ringInnerRadius, ringOuterRadius, textScale, sections, deadZone);
    }

    public WheelWidget(
        int x,
        int y,
        int width,
        int height,
        float ringInnerRadius,
        float ringOuterRadius,
        float textScale,
        float degreeOffsetAngle,
        List<RawSection> sections,
        int deadZone
    ) {
        this(x, y, width, height, Component.empty(), ringInnerRadius, ringOuterRadius, textScale, degreeOffsetAngle, sections, deadZone);
    }

    public WheelWidget(int x, int y, int width, int height, float ringInnerRadius, float ringOuterRadius, List<RawSection> sections, int deadZone) {
        this(x, y, width, height, Component.empty(), ringInnerRadius, ringOuterRadius, sections, deadZone);
    }

    public WheelWidget(
        int x,
        int y,
        int width,
        int height,
        Component message,
        float ringInnerRadius,
        float ringOuterRadius,
        float textScale,
        float degreeOffsetAngle,
        List<RawSection> sections,
        int deadZone
    ) {
        this(
            x,
            y,
            width,
            height,
            message,
            ringInnerRadius,
            ringOuterRadius,
            150,
            300,
            150,
            0x88000000,
            DEFAULT_SELECTION_EFFECT_COLOR,
            5f,
            0xfdfdfd,
            textScale,
            degreeOffsetAngle,
            sections,
            deadZone
        );
    }

    public WheelWidget(
        int x,
        int y,
        int width,
        int height,
        Component message,
        float ringInnerRadius,
        float ringOuterRadius,
        List<RawSection> sections,
        int deadZone
    ) {
        this(
            x,
            y,
            width,
            height,
            message,
            ringInnerRadius,
            ringOuterRadius,
            150,
            300,
            150,
            0x88000000,
            DEFAULT_SELECTION_EFFECT_COLOR,
            5f,
            0xfdfdfd,
            1f,
            0f,
            sections,
            deadZone
        );
    }

    public WheelWidget(
        int x,
        int y,
        int width,
        int height,
        Component message,
        float ringInnerRadius,
        float ringOuterRadius,
        float degreeOffsetAngle,
        List<RawSection> sections,
        int deadZone
    ) {
        this(
            x,
            y,
            width,
            height,
            message,
            ringInnerRadius,
            ringOuterRadius,
            150,
            300,
            150,
            0x88000000,
            DEFAULT_SELECTION_EFFECT_COLOR,
            5f,
            0xfdfdfd,
            1f,
            degreeOffsetAngle,
            sections,
            deadZone
        );
    }

    public WheelWidget(
        int x,
        int y,
        int width,
        int height,
        float ringInnerRadius,
        float ringOuterRadius,
        int delay,
        int animationMs,
        int closingAnimationMs,
        int ringColor,
        int selectionEffectColor,
        float selectionAnimationSpeedFactor,
        int textColor,
        float textScale,
        List<RawSection> sections,
        int deadZone
    ) {
        this(
            x,
            y,
            width,
            height,
            Component.empty(),
            ringInnerRadius,
            ringOuterRadius,
            delay,
            animationMs,
            closingAnimationMs,
            ringColor,
            selectionEffectColor,
            selectionAnimationSpeedFactor,
            textColor,
            textScale,
            0f,
            sections,
            deadZone
        );
    }

    public WheelWidget(
        int x,
        int y,
        int width,
        int height,
        Component message,
        float ringInnerRadius,
        float ringOuterRadius,
        int delay,
        int animationMs,
        int closingAnimationMs,
        int ringColor,
        int selectionEffectColor,
        float selectionAnimationSpeedFactor,
        int textColor,
        float textScale,
        float degreeOffsetAngle,
        List<RawSection> sections,
        int deadZone
    ) {
        super(x, y, width, height, message);
        this.centerPos = new Vector2f(this.getX() + this.getWidth() / 2f, this.getY() + this.getHeight() / 2f);
        this.ringInnerRadius = Math.max(ringInnerRadius, IGNORE_CURSOR_MOVE_LENGTH);
        this.ringOuterRadius = ringOuterRadius;
        this.delay = delay;
        this.animationMs = animationMs;
        this.closingAnimationMs = closingAnimationMs;
        this.ringColor = ringColor;
        this.selectionEffectColor = selectionEffectColor;
        this.selectionAnimationSpeedFactor = selectionAnimationSpeedFactor;
        this.textColor = textColor;
        this.textScale = textScale;
        this.deadZone = deadZone;
        float degreeEachRotation = 360f / sections.size();
        for (int i = 0; i < sections.size(); i++) {
            RawSection section = sections.get(i);
            float rotation = MathUtil.clampWithProportion((degreeEachRotation * i + degreeOffsetAngle) % 360, 0, 360);
            Vector2f rotated = MathUtil.rotationDegrees(ROTATION_START, rotation)
                .mul(1, -1)
                .mul(this.getSectionCircleRadius())
                .add(this.centerPos);
            float detectionStart = (float) (Math.toRadians(rotation - degreeEachRotation / 2f) + Math.PI * 2);
            float detectionEnd = (float) (Math.toRadians(rotation + degreeEachRotation / 2f) + Math.PI * 2);
            detectionStart = detectionStart % (float) (Math.PI * 2);
            detectionEnd = detectionEnd % (float) (Math.PI * 2);
            this.sections.add(new WheelSection(
                rotated,
                (float) (Math.toRadians(rotation) % (Math.PI * 2)),
                detectionStart,
                detectionEnd,
                section
            ));
        }
        this.selectionEffectPos = MathUtil.rotate(MathUtil.copy(ROTATION_START).mul(this.getSectionCircleRadius()), this.currentAngle);
    }

    public void renderDisc(
        GuiGraphics guiGraphics,
        float progress
    ) {
        this.renderDisc(guiGraphics, this.centerPos.x, this.centerPos.y, this.getDiscRadius() * progress, this.ringColor);
    }

    public void renderFrostedBackground(
        GuiGraphics guiGraphics,
        float progress
    ) {
        if (this.frostedBackground == null) {
            return;
        }
        try {
            Window window = this.minecraft.getWindow();
            float guiScale = (float) window.getGuiScale();
            float radius = this.getDiscRadius() * progress * guiScale;
            if (radius <= 0) {
                return;
            }
            int blurredTextureId = this.frostedBackground.capture(guiGraphics);
            if (blurredTextureId < 0) {
                return;
            }
            Matrix4f matrix4f = guiGraphics.pose().last().pose();
            float w = (float) window.getGuiScaledWidth();
            float h = (float) window.getGuiScaledHeight();
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            // 全屏 quad：GUI 顶部 (y=0) 对应纹理 v=1（纹理数据顶部），做 V 轴翻转
            bufferBuilder.addVertex(matrix4f, 0, 0, DISC_Z).setUv(0, 1).setColor(FROSTED_TINT);
            bufferBuilder.addVertex(matrix4f, 0, h, DISC_Z).setUv(0, 0).setColor(FROSTED_TINT);
            bufferBuilder.addVertex(matrix4f, w, h, DISC_Z).setUv(1, 0).setColor(FROSTED_TINT);
            bufferBuilder.addVertex(matrix4f, w, 0, DISC_Z).setUv(1, 1).setColor(FROSTED_TINT);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            ShaderInstance frostedDiscShader = LibShaders.getFrostedDiscShader();
            if (frostedDiscShader == null) {
                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                log.error("Wheel frosted disc shader is not ready");
                return;
            }
            RenderSystem.setShaderTexture(0, blurredTextureId);
            RenderSystem.setShader(() -> frostedDiscShader);
            frostedDiscShader.safeGetUniform("Center").set(this.centerPos.x * guiScale, this.centerPos.y * guiScale);
            frostedDiscShader.safeGetUniform("FramebufferSize").set((float) window.getWidth(), (float) window.getHeight());
            frostedDiscShader.safeGetUniform("Radius").set(radius);
            frostedDiscShader.safeGetUniform("AntiAliasingRadius").set(1.25f);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        } catch (Exception e) {
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            log.error("Wheel frosted background failed to render", e);
        }
    }

    public void renderSeparatorRing(
        GuiGraphics guiGraphics,
        float progress
    ) {
        // SdfGraphics.stroke 实际厚度为入参一半，此处直接以厚度/2 作为环带半宽
        float radius = (this.ringInnerRadius + SEPARATOR_RING_GAP) * progress;
        float halfThickness = SEPARATOR_RING_THICKNESS * 0.5f;
        WheelWidget.renderRing(
            guiGraphics,
            this.centerPos.x,
            this.centerPos.y,
            0xFFFFFFFF,
            radius - halfThickness,
            radius + halfThickness
        );
    }

    public void renderSelectionEffect(GuiGraphics guiGraphics, float centerX, float centerY, int color, float ringWidth) {
        float dotDiameter = this.getSelectionDotDiameter(ringWidth) * 2.0f;
        float dotRadius = dotDiameter * 0.5f;
        Window window = this.minecraft.getWindow();
        float guiScale = (float) window.getGuiScale();
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_COLOR
        );
        float x1 = centerX - dotRadius - 5;
        float y1 = centerY - dotRadius - 5;
        float x2 = centerX + dotRadius + 5;
        float y2 = centerY + dotRadius + 5;
        bufferBuilder.addVertex(matrix4f, x1, y1, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x1, y2, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y2, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y1, SELECTION_Z).setColor(color);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderInstance selectionShader = LibShaders.getSelectionShader();
        if (selectionShader == null) {
            this.renderSelectionFallback(guiGraphics, centerX, centerY, color, dotRadius);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            return;
        }
        RenderSystem.setShader(() -> selectionShader);
        selectionShader
            .safeGetUniform("Center")
            .set(centerX * guiScale, centerY * guiScale);
        selectionShader
            .safeGetUniform("FramebufferSize")
            .set((float) window.getWidth(), (float) window.getHeight());
        selectionShader
            .safeGetUniform("Radius")
            .set(dotRadius * guiScale);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public void renderAnnularSectorSelection(
        GuiGraphics guiGraphics,
        float centerX,
        float centerY,
        int color,
        float innerRadius,
        float outerRadius,
        float centerAngleRad,
        float rangeAngleRad
    ) {
        float boundsRadius = outerRadius + 5;
        float x1 = centerX - boundsRadius;
        float y1 = centerY - boundsRadius;
        float x2 = centerX + boundsRadius;
        float y2 = centerY + boundsRadius;
        Window window = this.minecraft.getWindow();
        float guiScale = (float) window.getGuiScale();
        // Wheel section angle uses "up" as zero; shader atan uses +X as zero.
        float shaderCenterAngle = centerAngleRad + TAU / 4.0f;
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_COLOR
        );
        bufferBuilder.addVertex(matrix4f, x1, y1, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x1, y2, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y2, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y1, SELECTION_Z).setColor(color);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderInstance annularSectorShader = LibShaders.getAnnularSectorShader();
        if (annularSectorShader == null) {
            this.renderAnnularSectorFallback(
                guiGraphics,
                centerX,
                centerY,
                color,
                innerRadius,
                outerRadius,
                centerAngleRad,
                rangeAngleRad
            );
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            return;
        }
        RenderSystem.setShader(() -> annularSectorShader);
        annularSectorShader
            .safeGetUniform("Center")
            .set(centerX * guiScale, centerY * guiScale);
        annularSectorShader
            .safeGetUniform("InnerRadius")
            .set(innerRadius * guiScale);
        annularSectorShader
            .safeGetUniform("OuterRadius")
            .set(outerRadius * guiScale);
        annularSectorShader
            .safeGetUniform("AntiAliasingRadius")
            .set(1.25f);
        annularSectorShader
            .safeGetUniform("AngleAntiAliasingRad")
            .set(ANGLE_AA_RAD);
        annularSectorShader
            .safeGetUniform("CenterAngleRad")
            .set(shaderCenterAngle);
        annularSectorShader
            .safeGetUniform("RangeAngleRad")
            .set(rangeAngleRad);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    public WheelWidget setSelectionEffect(WheelSelectionEffect selectionEffect) {
        this.selectionEffect = Objects.requireNonNull(selectionEffect, "selectionEffect");
        return this;
    }

    public WheelWidget setSelectionEffectColor(int selectionEffectColor) {
        this.selectionEffectColor = selectionEffectColor;
        return this;
    }

    public WheelWidget setFrostedBackground(@Nullable WheelFrostedBackground frostedBackground) {
        this.frostedBackground = frostedBackground;
        return this;
    }

    public WheelWidget setPageState(boolean hasPreviousPage, boolean hasNextPage) {
        this.hasPreviousPage = hasPreviousPage;
        this.hasNextPage = hasNextPage;
        return this;
    }

    public WheelWidget setCurrentIndex(int index) {
        if (index < 0 || index >= this.sections.size()) return this;
        if (!this.sections.get(index).selectable()) return this;
        this.setCurrentSectionIndex(index);
        this.currentAngle = this.sections.get(index).angle;
        this.selectionEffectPos = MathUtil.rotate(MathUtil.copy(ROTATION_START).mul(this.getSectionCircleRadius()), this.currentAngle);
        return this;
    }

    public WheelWidget clearSelection() {
        this.setCurrentSectionIndex(-1);
        return this;
    }

    public float getSectionCircleRadius() {
        // 图标圆心所在圆周的半径：分隔圆环与盘面外缘之间扇区的中间位置
        return (this.ringOuterRadius + this.ringInnerRadius) * 0.5f;
    }

    private float getDiscRadius() {
        return this.ringOuterRadius;
    }

    public int getSectionSize() {
        return this.sections.size();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.sections.stream().noneMatch(WheelSection::selectable)) {
            return true;
        }

        int index = this.currentSectionIndex;
        if (index < 0 || index >= this.sections.size()) {
            index = scrollY > 0 ? -1 : 0;
        }

        if (scrollY > 0) {
            this.setCurrentSectionIndex(this.findNextSelectableIndex(index, 1));
        } else if (scrollY < 0) {
            this.setCurrentSectionIndex(this.findNextSelectableIndex(index, -1));
        }

        for (WheelSection section : this.sections) {
            if (this.sections.indexOf(section) == this.currentSectionIndex) {
                this.currentAngle = section.angle;
                return true;
            }
        }
        return true;
    }

    public void checkMousePos(double mouseX, double mouseY) {
        if (this.closingAnimationStarted) return;
        float centerX = this.centerPos.x;
        float centerY = this.centerPos.y;
        Vector2f cursorPos = new Vector2f((float) mouseX - centerX, (float) mouseY - centerY);
        if (cursorPos.length() < this.deadZone) {
            this.mouseAngleRad = null;
            this.setCurrentSectionIndex(-1);
            return;
        }
        Vector2f rotationStart = new Vector2f(0, 1);
        cursorPos.normalize();
        double rot = Math.acos(rotationStart.dot(cursorPos) / (rotationStart.length() * cursorPos.length()));
        double rotation = cursorPos.x < 0 ? Math.PI - rot : Math.PI + rot;
        this.mouseAngleRad = (float) (rotation % (Math.PI * 2));
        for (WheelSection section : this.sections) {
            // 跨越 0° 的扇区由圆周末尾与开头两段组成。
            boolean containsRotation = section.angleStart > section.angleEnd
                ? rotation >= section.angleStart || rotation <= section.angleEnd
                : rotation >= section.angleStart && rotation <= section.angleEnd;
            if (containsRotation && section.selectable) {
                this.currentAngle = section.angle;
                this.setCurrentSectionIndex(this.sections.indexOf(section));
                break;
            }
        }
    }

    private void setCurrentSectionIndex(int index) {
        if (this.currentSectionIndex == index) {
            return;
        }
        this.currentSectionIndex = index;
        this.selectionChangeTime = System.currentTimeMillis();
    }

    private int findNextSelectableIndex(int start, int direction) {
        if (this.sections.isEmpty()) {
            return -1;
        }
        int idx = start;
        for (int i = 0; i < this.sections.size(); i++) {
            idx = (idx + direction + this.sections.size()) % this.sections.size();
            if (this.sections.get(idx).selectable) {
                return idx;
            }
        }
        return -1;
    }

    public boolean shouldRender() {
        if (this.animationStarted) return true;
        return (this.displayTime + this.delay) <= System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.checkMousePos(mouseX, mouseY);
        this.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderClosingAnimation(guiGraphics);
        if (!this.shouldRender()) {
            return;
        }
        if (this.closingAnimationStarted) return;
        if (!this.animationStarted) {
            this.animationStarted = true;
            this.displayTime = System.currentTimeMillis();
        }
        final PoseStack poseStack = guiGraphics.pose();
        float delta = this.displayTime + this.animationMs - System.currentTimeMillis();
        if (delta > 0) {
            float progress = 1 - (delta / this.animationMs);
            progress = (float) (-Math.pow(progress, 2) + 2 * progress);
            if (progress == 0) return;
            this.renderProgressAnimation(guiGraphics, progress);
            return;
        }

        this.renderFrostedBackground(guiGraphics, 1f);
        this.renderDisc(guiGraphics, 1f);
        this.renderSeparatorRing(guiGraphics, 1f);
        this.renderHoverChevron(guiGraphics);
        if (this.currentSectionIndex != -1) {
            this.renderSelection(guiGraphics);
        }
        for (WheelSection value : this.sections) {
            float x = value.center.x;
            float y = value.center.y;
            var renderer = value.renderer();
            if (renderer != null) {
                poseStack.pushPose();
                poseStack.translate(x, y, 0);
                int renderSize = this.getRendererSize(this.ringOuterRadius - this.ringInnerRadius);
                renderer.render(guiGraphics, poseStack, renderSize, renderSize);
                poseStack.popPose();
            }
        }
        this.renderCenterTitle(guiGraphics);
        this.renderPageArrows(guiGraphics);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public void renderClosingAnimation(GuiGraphics guiGraphics) {
        if (!this.closingAnimationStarted) return;
        float delta = this.displayTime + this.closingAnimationMs - System.currentTimeMillis();
        float progress = delta / this.closingAnimationMs;
        if (progress >= 1 || progress <= 0) {
            this.minecraft.setScreen(null);
        }
        this.renderProgressAnimation(guiGraphics, progress);
    }

    private void renderProgressAnimation(GuiGraphics guiGraphics, float progress) {
        progress = (float) (-Math.pow(progress, 2) + 2 * progress);
        if (progress == 0) return;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        this.renderFrostedBackground(guiGraphics, progress);
        this.renderDisc(guiGraphics, progress);
        this.renderSeparatorRing(guiGraphics, progress);
        poseStack.popPose();
        if (this.currentSectionIndex != -1) {
            if (this.selectionEffect == WheelSelectionEffect.ANNULAR_SECTOR) {
                WheelSection section = this.sections.get(this.currentSectionIndex);
                float rangeAngle = this.normalizePositiveAngle(section.angleEnd - section.angleStart) / 2.0f;
                float settle = this.settleProgress();
                float expand = settle * SETTLE_EXPAND;
                this.renderAnnularSectorSelection(
                    guiGraphics,
                    this.centerPos.x,
                    this.centerPos.y,
                    this.getSelectionSectorColor(settle),
                    (this.ringInnerRadius + SECTION_INNER_INSET + expand) * progress,
                    (this.ringOuterRadius - SECTION_OUTER_INSET + expand) * progress,
                    section.angle,
                    rangeAngle
                );
            } else {
                WheelSection section = this.sections.get(this.currentSectionIndex);
                Vector2f center = new Vector2f(
                    (section.center.x - this.centerPos.x) / this.getSectionCircleRadius(),
                    (section.center.y - this.centerPos.y) / this.getSectionCircleRadius()
                ).mul(this.getSectionCircleRadius() * progress).add(this.centerPos.x, this.centerPos.y);
                this.renderSelectionEffect(
                    guiGraphics,
                    center.x,
                    center.y,
                    this.selectionEffectColor,
                    (this.ringOuterRadius - this.ringInnerRadius) * progress
                );
            }
        }
        for (WheelSection value : this.sections) {
            Vector2f center = new Vector2f(
                (value.center.x - this.centerPos.x) / this.getSectionCircleRadius(),
                (value.center.y - this.centerPos.y) / this.getSectionCircleRadius()
            ).mul(this.getSectionCircleRadius() * progress).add(this.centerPos.x, this.centerPos.y);
            float x = center.x;
            float y = center.y;
            var renderer = value.renderer();
            if (renderer != null) {
                poseStack.pushPose();
                poseStack.translate(x, y, 0);
                int renderSize = this.getRendererSize((this.ringOuterRadius - this.ringInnerRadius) * progress);
                renderer.render(guiGraphics, poseStack, renderSize, renderSize);
                poseStack.popPose();
            }
        }
        if (this.currentSectionIndex != -1) {
            WheelSection section = this.sections.get(this.currentSectionIndex);
            Component title = section.subTitle();
            if (title != null && !title.getString().isEmpty()) {
                final int textAlpha = (int) (progress * 0xff) << 24;
                final int shadowAlpha = (int) (progress * 0x99) << 24;
                poseStack.pushPose();
                poseStack.translate(this.centerPos.x, this.centerPos.y, 0);
                float scale = this.textScale * CENTER_TITLE_SCALE;
                poseStack.scale(scale, scale, 1f);
                int textY = -this.minecraft.font.lineHeight / 2;
                guiGraphics.drawCenteredString(this.minecraft.font, title, 1, textY + 1, shadowAlpha);
                guiGraphics.drawCenteredString(this.minecraft.font, title, 0, textY, textAlpha | 0xfdfdfd);
                poseStack.popPose();
            }
        }
    }

    private void renderSelection(GuiGraphics guiGraphics) {
        if (this.selectionEffect == WheelSelectionEffect.ANNULAR_SECTOR) {
            this.renderSelectionAnnularSector(guiGraphics);
            return;
        }
        float selectionEffectAngle = MathUtil.angle(MathUtil.copy(ROTATION_START), this.selectionEffectPos);

        float diffAngle = this.currentAngle - selectionEffectAngle;

        if (diffAngle > Math.PI) {
            diffAngle -= (float) (Math.PI * 2);
        } else if (diffAngle < -Math.PI) {
            diffAngle += (float) (Math.PI * 2);
        }

        this.selectionEffectPos = MathUtil.rotate(this.selectionEffectPos, diffAngle / this.selectionAnimationSpeedFactor);

        Vector2f pos = MathUtil.copy(this.selectionEffectPos).mul(1, -1).add(this.centerPos);

        this.renderSelectionEffect(
            guiGraphics,
            pos.x,
            pos.y,
            this.selectionEffectColor,
            this.ringOuterRadius - this.ringInnerRadius
        );
    }

    private void renderSelectionAnnularSector(GuiGraphics guiGraphics) {
        WheelSection currentSection = this.sections.get(this.currentSectionIndex);
        // 渲染角度沿最短路径向目标扇区角度平滑逼近，高亮在两个扇区间滑动
        float diffAngle = this.normalizeSignedAngle(currentSection.angle - this.selectionAngleRad);
        this.selectionAngleRad += diffAngle / this.selectionAnimationSpeedFactor;

        float rangeAngle = this.normalizePositiveAngle(currentSection.angleEnd - currentSection.angleStart) / 2.0f;
        // 鼠标停住后，扇区内外缘同步向外扩张一小段距离（外缘最终贴合盘面外边缘），
        // 同时透明度从 20% 平滑提升到 100%
        float settle = this.settleProgress();
        float expand = settle * SETTLE_EXPAND;
        this.renderAnnularSectorSelection(
            guiGraphics,
            this.centerPos.x,
            this.centerPos.y,
            this.getSelectionSectorColor(settle),
            this.ringInnerRadius + SECTION_INNER_INSET + expand,
            this.ringOuterRadius - SECTION_OUTER_INSET + expand,
            this.selectionAngleRad,
            rangeAngle
        );
    }

    /**
     * 根据停住动画进度计算环扇高亮色：基础透明度为 20%，停住后平滑升至配置颜色的透明度。
     */
    private int getSelectionSectorColor(float settle) {
        int baseAlpha = (this.selectionEffectColor >>> 24) & 0xFF;
        int alpha = Math.round(baseAlpha * (0.2f + 0.8f * settle));
        return (alpha << 24) | (this.selectionEffectColor & 0xFFFFFF);
    }

    /**
     * 鼠标停住（选中扇区在 SETTLE_DELAY_MS 内未变化）后，0→1 的平滑外推动画进度。
     */
    private float settleProgress() {
        long elapsed = System.currentTimeMillis() - this.selectionChangeTime;
        float p = Mth.clamp((elapsed - SETTLE_DELAY_MS) / (float) SETTLE_ANIM_MS, 0.0f, 1.0f);
        return p * p * (3.0f - 2.0f * p);
    }

    private float normalizeSignedAngle(float angle) {
        float normalized = angle % TAU;
        if (normalized > Math.PI) {
            normalized -= TAU;
        } else if (normalized < -Math.PI) {
            normalized += TAU;
        }
        return normalized;
    }

    private void renderCenterTitle(GuiGraphics guiGraphics) {
        if (this.currentSectionIndex < 0) {
            return;
        }
        WheelSection section = this.sections.get(this.currentSectionIndex);
        Component title = section.subTitle();
        if (title == null || title.getString().isEmpty()) {
            return;
        }
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(this.centerPos.x, this.centerPos.y, 0);
        float scale = this.textScale * CENTER_TITLE_SCALE;
        poseStack.scale(scale, scale, 1f);
        // drawCenteredString 的 y 是文本顶部，向上偏移半个行高使文本垂直居中于轮盘中心
        int textY = -this.minecraft.font.lineHeight / 2;
        guiGraphics.drawCenteredString(this.minecraft.font, title, 1, textY + 1, 0x99000000);
        guiGraphics.drawCenteredString(this.minecraft.font, title, 0, textY, (0xff << 24) | this.textColor);
        poseStack.popPose();
    }

    private void renderPageArrows(GuiGraphics guiGraphics) {
        if (!this.hasPreviousPage && !this.hasNextPage) {
            return;
        }
        // 左右下角的箭头做轻微的水平浮动动画，提示可以翻页
        float phase = System.currentTimeMillis() / 1000f;
        float bob = (float) Math.sin(phase * 2.2f) * 4f;
        float cornerDist = this.getDiscRadius() * 0.7071f + 26f;
        float arrowY = this.centerPos.y + cornerDist;
        float leftX = this.centerPos.x - cornerDist + bob;
        float rightX = this.centerPos.x + cornerDist - bob;
        if (this.hasPreviousPage) {
            this.renderChevron(guiGraphics, leftX, arrowY, CHEVRON_SIZE, 0xFFFFFFFF, true);
        }
        if (this.hasNextPage) {
            this.renderChevron(guiGraphics, rightX, arrowY, CHEVRON_SIZE, 0xFFFFFFFF, false);
        }
    }

    private void renderChevron(GuiGraphics guiGraphics, float x, float y, float size, int color, boolean pointingLeft) {
        float halfThickness = CHEVRON_THICKNESS * 0.5f;
        if (pointingLeft) {
            this.renderSegment(guiGraphics, x + size, y - size, x - size, y, color, halfThickness);
            this.renderSegment(guiGraphics, x - size, y, x + size, y + size, color, halfThickness);
        } else {
            this.renderSegment(guiGraphics, x - size, y - size, x + size, y, color, halfThickness);
            this.renderSegment(guiGraphics, x + size, y, x - size, y + size, color, halfThickness);
        }
    }

    private float normalizePositiveAngle(float angle) {
        float normalized = angle % TAU;
        return normalized < 0 ? normalized + TAU : normalized;
    }

    /**
     * 在分隔圆环内侧渲染一个指向鼠标方向的尖括号箭头。
     */
    private void renderHoverChevron(GuiGraphics guiGraphics) {
        if (this.mouseAngleRad == null) {
            return;
        }
        // 与扇区排布同一坐标系换算屏幕方向：正上方为 0
        Vector2f dir = MathUtil.rotate(MathUtil.copy(ROTATION_START), this.mouseAngleRad).mul(1, -1);
        Vector2f perp = new Vector2f(-dir.y, dir.x);
        float tipDist = this.ringInnerRadius - HOVER_CHEVRON_RING_GAP;
        Vector2f tip = new Vector2f(dir).mul(tipDist).add(this.centerPos);
        Vector2f base = new Vector2f(dir).mul(tipDist - HOVER_CHEVRON_SIZE).add(this.centerPos);
        Vector2f wingA = new Vector2f(base).add(new Vector2f(perp).mul(HOVER_CHEVRON_SIZE * 0.8f));
        Vector2f wingB = new Vector2f(base).add(new Vector2f(perp).mul(-HOVER_CHEVRON_SIZE * 0.8f));
        float halfThickness = HOVER_CHEVRON_THICKNESS * 0.5f;
        this.renderSegment(guiGraphics, wingA.x, wingA.y, tip.x, tip.y, 0xFFFFFFFF, halfThickness);
        this.renderSegment(guiGraphics, wingB.x, wingB.y, tip.x, tip.y, 0xFFFFFFFF, halfThickness);
    }

    private void renderSegment(
        GuiGraphics guiGraphics,
        float x0,
        float y0,
        float x1,
        float y1,
        int color,
        float halfThickness
    ) {
        Window window = this.minecraft.getWindow();
        float guiScale = (float) window.getGuiScale();
        float margin = halfThickness + 5;
        float minX = Math.min(x0, x1) - margin;
        float minY = Math.min(y0, y1) - margin;
        float maxX = Math.max(x0, x1) + margin;
        float maxY = Math.max(y0, y1) + margin;
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(matrix4f, minX, minY, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, minX, maxY, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, maxX, maxY, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, maxX, minY, SELECTION_Z).setColor(color);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderInstance segmentShader = LibShaders.getSegmentShader();
        if (segmentShader == null) {
            this.renderSegmentFallback(guiGraphics, x0, y0, x1, y1, color, halfThickness);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            return;
        }
        RenderSystem.setShader(() -> segmentShader);
        segmentShader.safeGetUniform("Point0").set(x0 * guiScale, y0 * guiScale);
        segmentShader.safeGetUniform("Point1").set(x1 * guiScale, y1 * guiScale);
        segmentShader.safeGetUniform("LineWidth").set(halfThickness * 2f * guiScale);
        segmentShader.safeGetUniform("FramebufferSize").set((float) window.getWidth(), (float) window.getHeight());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private float getSelectionDotDiameter(float ringWidth) {
        return ringWidth * SELECTION_DOT_DIAMETER_RATIO;
    }

    private int getRendererSize(float ringWidth) {
        return Math.max(1, Math.round(this.getSelectionDotDiameter(ringWidth)));
    }

    public void onClosing() {
        if (this.shouldRender() && !this.closingAnimationStarted) {
            this.displayTime = System.currentTimeMillis();
            this.closingAnimationStarted = true;
        } else {
            this.minecraft.setScreen(null);
        }
    }

    private void renderDisc(GuiGraphics guiGraphics, float centerX, float centerY, float radius, int color) {
        Window window = this.minecraft.getWindow();
        float guiScale = (float) window.getGuiScale();
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_COLOR
        );
        float x1 = centerX - radius - 5;
        float y1 = centerY - radius - 5;
        float x2 = centerX + radius + 5;
        float y2 = centerY + radius + 5;
        bufferBuilder.addVertex(matrix4f, x1, y1, DISC_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x1, y2, DISC_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y2, DISC_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y1, DISC_Z).setColor(color);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderInstance discShader = LibShaders.getDiscShader();
        if (discShader == null) {
            this.renderDiscFallback(guiGraphics, centerX, centerY, radius, color);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            return;
        }
        RenderSystem.setShader(() -> discShader);
        discShader.safeGetUniform("Center").set(centerX * guiScale, centerY * guiScale);
        discShader.safeGetUniform("FramebufferSize").set((float) window.getWidth(), (float) window.getHeight());
        discShader.safeGetUniform("Radius").set(radius * guiScale);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private void renderDiscFallback(GuiGraphics guiGraphics, float centerX, float centerY, float radius, int centerColor) {
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.TRIANGLE_FAN,
            DefaultVertexFormat.POSITION_COLOR
        );
        int edgeColor = centerColor & 0x00FFFFFF;
        int segments = 48;
        bufferBuilder.addVertex(matrix4f, centerX, centerY, DISC_Z).setColor(centerColor);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 2 * i / segments);
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            bufferBuilder.addVertex(matrix4f, centerX + cos * radius, centerY + sin * radius, DISC_Z).setColor(edgeColor);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
    }

    private void renderSelectionFallback(GuiGraphics guiGraphics, float centerX, float centerY, int color, float radius) {
        this.renderDiscFallback(guiGraphics, centerX, centerY, radius, color);
    }

    private void renderAnnularSectorFallback(
        GuiGraphics guiGraphics,
        float centerX,
        float centerY,
        int color,
        float innerRadius,
        float outerRadius,
        float centerAngleRad,
        float rangeAngleRad
    ) {
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.TRIANGLE_STRIP,
            DefaultVertexFormat.POSITION_COLOR
        );
        int segments = 48;
        float startAngle = centerAngleRad - rangeAngleRad;
        float sweepAngle = rangeAngleRad * 2;
        for (int i = 0; i <= segments; i++) {
            float angle = startAngle + sweepAngle * i / segments;
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            bufferBuilder.addVertex(matrix4f, centerX + cos * outerRadius, centerY + sin * outerRadius, SELECTION_Z).setColor(color);
            bufferBuilder.addVertex(matrix4f, centerX + cos * innerRadius, centerY + sin * innerRadius, SELECTION_Z).setColor(color);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
    }

    private void renderSegmentFallback(
        GuiGraphics guiGraphics,
        float x0,
        float y0,
        float x1,
        float y1,
        int color,
        float halfThickness
    ) {
        Vector2f dir = new Vector2f(x1 - x0, y1 - y0);
        float length = dir.length();
        if (length <= 0) {
            log.warn("Wheel segment fallback skipped: zero-length segment");
            return;
        }
        dir.div(length);
        Vector2f perp = new Vector2f(-dir.y, dir.x).mul(halfThickness);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.TRIANGLE_STRIP,
            DefaultVertexFormat.POSITION_COLOR
        );
        bufferBuilder.addVertex(matrix4f, x0 + perp.x, y0 + perp.y, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x0 - perp.x, y0 - perp.y, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x1 + perp.x, y1 + perp.y, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x1 - perp.x, y1 - perp.y, SELECTION_Z).setColor(color);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
    }

    public static void renderRing(
        GuiGraphics guiGraphics,
        float centerX,
        float centerY,
        int color,
        float innerDiameter,
        float outerDiameter
    ) {
        PoseStack poseStack = guiGraphics.pose();
        Matrix4f matrix4f = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_COLOR
        );
        float x1 = centerX - outerDiameter - 5;
        float y1 = centerY - outerDiameter - 5;
        float x2 = centerX + outerDiameter + 5;
        float y2 = centerY + outerDiameter + 5;
        bufferBuilder.addVertex(matrix4f, x1, y1, RING_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x1, y2, RING_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y2, RING_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y1, RING_Z).setColor(color);

        Window window = Minecraft.getInstance().getWindow();
        float guiScale = (float) window.getGuiScale();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderInstance ringShader = LibShaders.getRingShader();
        if (ringShader == null) {
            renderRingFallback(guiGraphics, centerX, centerY, color, innerDiameter, outerDiameter);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            return;
        }
        RenderSystem.setShader(() -> ringShader);

        ringShader
            .safeGetUniform("Center")
            .set(centerX * guiScale, centerY * guiScale);
        ringShader
            .safeGetUniform("InnerDiameter")
            .set(innerDiameter * guiScale);
        ringShader
            .safeGetUniform("OuterDiameter")
            .set(outerDiameter * guiScale);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void renderRingFallback(
        GuiGraphics guiGraphics,
        float centerX,
        float centerY,
        int color,
        float innerDiameter,
        float outerDiameter
    ) {
        PoseStack poseStack = guiGraphics.pose();
        Matrix4f matrix4f = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.TRIANGLE_STRIP,
            DefaultVertexFormat.POSITION_COLOR
        );
        float innerRadius = innerDiameter / 2f;
        float outerRadius = outerDiameter / 2f;
        int segments = 96;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 2 * i / segments);
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            bufferBuilder.addVertex(matrix4f, centerX + cos * outerRadius, centerY + sin * outerRadius, RING_Z).setColor(color);
            bufferBuilder.addVertex(matrix4f, centerX + cos * innerRadius, centerY + sin * innerRadius, RING_Z).setColor(color);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
    }

    @FunctionalInterface
    public interface SectionRenderer {
        void render(GuiGraphics graphics, PoseStack pose, int width, int height);
    }

    public record WheelSection(
        Vector2f center, float angle, float angleStart, float angleEnd, Component subTitle,
        @Nullable SectionRenderer renderer, boolean selectable
    ) {
        public WheelSection(Vector2f center, float angle, float angleStart, float angleEnd, RawSection section) {
            this(center, angle, angleStart, angleEnd, section.name(), section.renderer(), section.selectable());
        }
    }

    public record RawSection(Component name, @Nullable SectionRenderer renderer, boolean selectable) {
        public RawSection(Component name, @Nullable SectionRenderer renderer) {
            this(name, renderer, true);
        }
    }

}
