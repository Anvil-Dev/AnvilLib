package dev.anvilcraft.lib.v2.wheel.client.gui.component;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Window;
import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.wheel.AnvilLibWheel;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.gui.render.state.AnnularSectorRenderState;
import dev.anvilcraft.lib.v2.wheel.client.gui.render.state.FrostedDiscRenderState;
import dev.anvilcraft.lib.v2.util.MathUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;
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
    // （SdfGraphics.stroke 实际厚度为入参一半）
    private static final float SEPARATOR_RING_GAP = 4f;
    private static final float SEPARATOR_RING_THICKNESS = 4f;
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
    private static final float HOVER_CHEVRON_THICKNESS = 8f;
    private static final float HOVER_CHEVRON_RING_GAP = 4f;
    // 中心悬停名缩放倍率
    private static final float CENTER_TITLE_SCALE = 1.4f;
    // 翻页箭头（尖括号）尺寸与线宽
    private static final float CHEVRON_SIZE = 8f;
    private static final float CHEVRON_THICKNESS = 6f;
    // 毛玻璃盘面叠加色：白 * 40% 透明度，混合在深色盘面上
    private static final int FROSTED_TINT = 0x66FFFFFF;

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
        GuiGraphicsExtractor guiGraphics,
        float progress
    ) {
        SdfGraphics.getInstance()
            .reset()
            .center(true)
            .color(this.ringColor)
            .circle(this.centerPos.x, this.centerPos.y, this.getDiscRadius() * progress)
            .fill()
            .draw(guiGraphics);
    }

    public void renderFrostedBackground(
        GuiGraphicsExtractor guiGraphics,
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
            GpuBufferSlice writeUniform = AnvilLibWheel.getLibDynamicUniforms().writeFrostedDisc(
                new Vector2f(window.getWidth(), window.getHeight()),
                new Vector2f(this.centerPos.x * guiScale, this.centerPos.y * guiScale),
                radius,
                1.25f
            );
            guiGraphics.submitGuiElementRenderState(new FrostedDiscRenderState(
                guiGraphics.pose(),
                0,
                0,
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight(),
                FROSTED_TINT,
                writeUniform,
                this.frostedBackground.capture(),
                guiGraphics.peekScissorStack()
            ));
        } catch (Exception e) {
            log.error("Wheel frosted background failed to render", e);
        }
    }

    public void renderSeparatorRing(
        GuiGraphicsExtractor guiGraphics,
        float progress
    ) {
        SdfGraphics.getInstance()
            .reset()
            .center(true)
            .color(0xFFFFFFFF)
            .circle(this.centerPos.x, this.centerPos.y, (this.ringInnerRadius + SEPARATOR_RING_GAP) * progress)
            .stroke(SEPARATOR_RING_THICKNESS)
            .fill()
            .draw(guiGraphics);
    }

    public void renderSelectionEffect(GuiGraphicsExtractor guiGraphics, float centerX, float centerY, int color, float ringWidth) {
        float dotDiameter = this.getSelectionDotDiameter(ringWidth) * 2.0f;
        float dotRadius = dotDiameter * 0.5f;
        SdfGraphics.getInstance()
            .center(true)
            .stroke(0)
            .color(color)
            .circle(centerX, centerY, 1f)
            .light(dotDiameter)
            .draw(guiGraphics);
    }

    public void renderAnnularSectorSelection(
        GuiGraphicsExtractor guiGraphics,
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
        GpuBufferSlice writeUniform = AnvilLibWheel.getLibDynamicUniforms().writeAnnularSector(
            new Vector2f(centerX * guiScale, centerY * guiScale),
            innerRadius * guiScale,
            outerRadius * guiScale,
            1.25f,
            ANGLE_AA_RAD,
            shaderCenterAngle,
            rangeAngleRad
        );
        guiGraphics.submitGuiElementRenderState(new AnnularSectorRenderState(
            guiGraphics.pose(),
            x1,
            y1,
            x2,
            y2,
            color,
            writeUniform,
            guiGraphics.peekScissorStack()
        ));
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
            if ((
                section.angleStart > section.angleEnd && rotation >= section.angleStart || rotation >= section.angleStart && rotation <= section.angleEnd
            ) && section.selectable) {
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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.checkMousePos(mouseX, mouseY);
        this.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderClosingAnimation(guiGraphics);
        if (!this.shouldRender()) {
            return;
        }
        if (this.closingAnimationStarted) return;
        if (!this.animationStarted) {
            this.animationStarted = true;
            this.displayTime = System.currentTimeMillis();
        }
        final Matrix3x2fStack poseStack = guiGraphics.pose();
        float delta = this.displayTime + this.animationMs - System.currentTimeMillis();
        if (delta > 0) {
            float progress = 1 - (delta / this.animationMs);
            progress = (float) (-Math.pow(progress, 2) + 2 * progress);
            if (progress == 0) return;
            this.renderProgressAnimation(guiGraphics, progress);
            return;
        }

        this.renderDisc(guiGraphics, 1f);
        this.renderFrostedBackground(guiGraphics, 1f);
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
                poseStack.pushMatrix();
                poseStack.translate(x, y);
                int renderSize = this.getRendererSize(this.ringOuterRadius - this.ringInnerRadius);
                renderer.render(guiGraphics, poseStack, renderSize, renderSize);
                poseStack.popMatrix();
            }
        }
        this.renderCenterTitle(guiGraphics);
        this.renderPageArrows(guiGraphics);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public void renderClosingAnimation(GuiGraphicsExtractor guiGraphics) {
        if (!this.closingAnimationStarted) return;
        float delta = this.displayTime + this.closingAnimationMs - System.currentTimeMillis();
        float progress = delta / this.closingAnimationMs;
        if (progress >= 1 || progress <= 0) {
            this.minecraft.setScreen(null);
        }
        this.renderProgressAnimation(guiGraphics, progress);
    }

    private void renderProgressAnimation(GuiGraphicsExtractor guiGraphics, float progress) {
        progress = (float) (-Math.pow(progress, 2) + 2 * progress);
        if (progress == 0) return;
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        this.renderDisc(guiGraphics, progress);
        this.renderFrostedBackground(guiGraphics, progress);
        this.renderSeparatorRing(guiGraphics, progress);
        poseStack.popMatrix();
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
                poseStack.pushMatrix();
                poseStack.translate(x, y);
                int renderSize = this.getRendererSize((this.ringOuterRadius - this.ringInnerRadius) * progress);
                renderer.render(guiGraphics, poseStack, renderSize, renderSize);
                poseStack.popMatrix();
            }
        }
        if (this.currentSectionIndex != -1) {
            WheelSection section = this.sections.get(this.currentSectionIndex);
            Component title = section.subTitle();
            if (title != null && !title.getString().isEmpty()) {
                final int textAlpha = (int) (progress * 0xff) << 24;
                final int shadowAlpha = (int) (progress * 0x99) << 24;
                poseStack.pushMatrix();
                poseStack.translate(this.centerPos.x, this.centerPos.y);
                float scale = this.textScale * CENTER_TITLE_SCALE;
                poseStack.scale(scale, scale);
                int textY = -this.minecraft.font.lineHeight / 2;
                guiGraphics.centeredText(this.minecraft.font, title, 1, textY + 1, shadowAlpha);
                guiGraphics.centeredText(this.minecraft.font, title, 0, textY, textAlpha | 0xfdfdfd);
                poseStack.popMatrix();
            }
        }
    }

    private void renderSelection(GuiGraphicsExtractor guiGraphics) {
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

    private void renderSelectionAnnularSector(GuiGraphicsExtractor guiGraphics) {
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

    private void renderCenterTitle(GuiGraphicsExtractor guiGraphics) {
        if (this.currentSectionIndex < 0) {
            return;
        }
        WheelSection section = this.sections.get(this.currentSectionIndex);
        Component title = section.subTitle();
        if (title == null || title.getString().isEmpty()) {
            return;
        }
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(this.centerPos.x, this.centerPos.y);
        float scale = this.textScale * CENTER_TITLE_SCALE;
        poseStack.scale(scale, scale);
        // centeredText 的 y 是文本顶部，向上偏移半个行高使文本垂直居中于轮盘中心
        int textY = -this.minecraft.font.lineHeight / 2;
        guiGraphics.centeredText(this.minecraft.font, title, 1, textY + 1, 0x99000000);
        guiGraphics.centeredText(this.minecraft.font, title, 0, textY, (0xff << 24) | this.textColor);
        poseStack.popMatrix();
    }

    private void renderPageArrows(GuiGraphicsExtractor guiGraphics) {
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

    private void renderChevron(GuiGraphicsExtractor guiGraphics, float x, float y, float size, int color, boolean pointingLeft) {
        SdfGraphics sdf = SdfGraphics.getInstance();
        if (pointingLeft) {
            sdf.reset().color(color).stroke(CHEVRON_THICKNESS)
                .segment(x + size, y - size, x - size, y).fill().draw(guiGraphics);
            sdf.reset().color(color).stroke(CHEVRON_THICKNESS)
                .segment(x - size, y, x + size, y + size).fill().draw(guiGraphics);
        } else {
            sdf.reset().color(color).stroke(CHEVRON_THICKNESS)
                .segment(x - size, y - size, x + size, y).fill().draw(guiGraphics);
            sdf.reset().color(color).stroke(CHEVRON_THICKNESS)
                .segment(x + size, y, x - size, y + size).fill().draw(guiGraphics);
        }
    }

    private float normalizePositiveAngle(float angle) {
        float normalized = angle % TAU;
        return normalized < 0 ? normalized + TAU : normalized;
    }

    /**
     * 在分隔圆环内侧渲染一个指向鼠标方向的尖括号箭头。
     */
    private void renderHoverChevron(GuiGraphicsExtractor guiGraphics) {
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
        SdfGraphics sdf = SdfGraphics.getInstance();
        sdf.reset().color(0xFFFFFFFF).stroke(HOVER_CHEVRON_THICKNESS)
            .segment(wingA.x, wingA.y, tip.x, tip.y).fill().draw(guiGraphics);
        sdf.reset().color(0xFFFFFFFF).stroke(HOVER_CHEVRON_THICKNESS)
            .segment(wingB.x, wingB.y, tip.x, tip.y).fill().draw(guiGraphics);
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

    @FunctionalInterface
    public interface SectionRenderer {
        void render(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, int width, int height);
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
