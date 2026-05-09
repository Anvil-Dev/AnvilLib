package dev.anvilcraft.lib.v2.wheel.client.gui.component;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Window;
import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.wheel.AnvilLibWheel;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.gui.render.state.AnnularSectorRenderState;
import dev.anvilcraft.lib.v2.util.MathUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

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
    private static final float RING_Z = 60f;
    private static final float SELECTION_Z = 80f;
    private static final Vector2f ROTATION_START = new Vector2f(0, 1);

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
    private Vector2f selectionEffectPos;
    private boolean animationStarted = false;
    @Getter
    @Setter
    private boolean closingAnimationStarted = false;
    private final int deadZone;
    private WheelSelectionEffect selectionEffect = WheelSelectionEffect.DOT;

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
                .mul(this.getSectionCircleDiameter())
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
        this.selectionEffectPos = MathUtil.rotate(MathUtil.copy(ROTATION_START).mul(this.getSectionCircleDiameter()), this.currentAngle);
    }

    public void renderRing(
        GuiGraphicsExtractor guiGraphics,
        float centerX,
        float centerY,
        int color,
        float innerDiameter,
        float outerDiameter,
        float mouseX,
        float mouseY
    ) {
        var width = outerDiameter - innerDiameter;
        var radius = outerDiameter - width * 0.5f;

        SdfGraphics ring = SdfGraphics.getInstance()
            .reset()
            .center(true)
            .circle(
                centerX,
                centerY,
                radius
            )
            .stroke(width * 2f);

        if (!ring.collide(mouseX, mouseY, 0.5f)) {
            ring.color(color);
        }

        ring.fill()
            .draw(guiGraphics);
    }

    public void renderRing(
        GuiGraphicsExtractor guiGraphics,
        float centerX,
        float centerY,
        int color,
        float innerDiameter,
        float outerDiameter
    ) {

        var width = outerDiameter - innerDiameter;
        var radius = outerDiameter - width * 0.5f;

        SdfGraphics ring = SdfGraphics.getInstance()
            .reset()
            .center(true)
            .color(0x99111111)
            .circle(
                centerX,
                centerY,
                radius
            )
            .stroke(width * 2f);


        ring.fill()
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
        float innerDiameter,
        float outerDiameter,
        float centerAngleRad,
        float rangeAngleRad
    ) {
        float outerRadius = outerDiameter + 5;
        float x1 = centerX - outerRadius;
        float y1 = centerY - outerRadius;
        float x2 = centerX + outerRadius;
        float y2 = centerY + outerRadius;
        Window window = this.minecraft.getWindow();
        float guiScale = (float) window.getGuiScale();
        // Wheel section angle uses "up" as zero; shader atan uses +X as zero.
        float shaderCenterAngle = centerAngleRad + TAU / 4.0f;
        GpuBufferSlice writeUniform = AnvilLibWheel.getLibDynamicUniforms().writeAnnularSector(
            new Vector2f(centerX * guiScale, centerY * guiScale),
            innerDiameter * guiScale,
            outerDiameter * guiScale,
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

    public WheelWidget setCurrentIndex(int index) {
        if (index < 0 || index >= this.sections.size()) return this;
        if (!this.sections.get(index).selectable()) return this;
        this.currentSectionIndex = index;
        this.currentAngle = this.sections.get(index).angle;
        this.selectionEffectPos = MathUtil.rotate(MathUtil.copy(ROTATION_START).mul(this.getSectionCircleDiameter()), this.currentAngle);
        return this;
    }

    public WheelWidget clearSelection() {
        this.currentSectionIndex = -1;
        return this;
    }

    public float getSectionCircleDiameter() {
        return this.ringOuterRadius - this.ringInnerRadius + this.ringInnerRadius * 2;
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
            this.currentSectionIndex = this.findNextSelectableIndex(index, 1);
        } else if (scrollY < 0) {
            this.currentSectionIndex = this.findNextSelectableIndex(index, -1);
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
            this.currentSectionIndex = -1;
            return;
        }
        Vector2f rotationStart = new Vector2f(0, 1);
        cursorPos.normalize();
        double rot = Math.acos(rotationStart.dot(cursorPos) / (rotationStart.length() * cursorPos.length()));
        double rotation = cursorPos.x < 0 ? Math.PI - rot : Math.PI + rot;
        for (WheelSection section : this.sections) {
            if ((
                section.angleStart > section.angleEnd && rotation >= section.angleStart || rotation >= section.angleStart && rotation <= section.angleEnd
            ) && section.selectable) {
                this.currentAngle = section.angle;
                this.currentSectionIndex = this.sections.indexOf(section);
                break;
            }
        }
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

        this.renderRing(
            guiGraphics,
            this.centerPos.x,
            this.centerPos.y,
            this.ringColor,
            this.ringInnerRadius * 2,
            this.ringOuterRadius * 2
        );
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
            poseStack.pushMatrix();
            float coordinateScale = 0.7f;
            float offsetX = 0.1f * this.width;
            float offsetY = 0.1f * this.height;
            float adjustedX = (x - offsetX) / coordinateScale;
            float adjustedY = renderer == null
                ? (y - offsetY - (minecraft.font.lineHeight * this.textScale) / 2.0f) / coordinateScale
                : (y - offsetY - 20 * this.textScale) / coordinateScale;

            poseStack.translate(offsetX, offsetY);
            poseStack.scale(coordinateScale, coordinateScale);
            poseStack.translate(adjustedX, adjustedY);
            poseStack.scale(this.textScale / coordinateScale, this.textScale / coordinateScale);
            guiGraphics.centeredText(minecraft.font, value.subTitle, 0, 0, (0xff << 24) | this.textColor);
            poseStack.popMatrix();
        }
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
        this.renderRing(
            guiGraphics,
            this.centerPos.x,
            this.centerPos.y,
            this.ringColor,
            this.ringInnerRadius * 2 * progress,
            this.ringOuterRadius * 2 * progress
        );
        poseStack.popMatrix();
        if (this.currentSectionIndex != -1) {
            if (this.selectionEffect == WheelSelectionEffect.ANNULAR_SECTOR) {
                WheelSection section = this.sections.get(this.currentSectionIndex);
                float rangeAngle = this.normalizePositiveAngle(section.angleEnd - section.angleStart) / 2.0f;
                this.renderAnnularSectorSelection(
                    guiGraphics,
                    this.centerPos.x,
                    this.centerPos.y,
                    this.selectionEffectColor,
                    this.ringInnerRadius * 2 * progress,
                    this.ringOuterRadius * 2 * progress,
                    section.angle,
                    rangeAngle
                );
            } else {
                WheelSection section = this.sections.get(this.currentSectionIndex);
                Vector2f center = new Vector2f(
                    (section.center.x - this.centerPos.x) / this.getSectionCircleDiameter(),
                    (section.center.y - this.centerPos.y) / this.getSectionCircleDiameter()
                ).mul(this.getSectionCircleDiameter() * progress).add(this.centerPos.x, this.centerPos.y);
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
                (value.center.x - this.centerPos.x) / this.getSectionCircleDiameter(),
                (value.center.y - this.centerPos.y) / this.getSectionCircleDiameter()
            ).mul(this.getSectionCircleDiameter() * progress).add(this.centerPos.x, this.centerPos.y);
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
            final int textAlpha = (int) (progress * 0xff) << 24;
            poseStack.pushMatrix();
            float coordinateScale = 0.7f;
            float offsetX = 0.1f * this.width;
            float offsetY = 0.1f * this.height;
            float adjustedX = (x - offsetX) / coordinateScale;
            float adjustedY = renderer == null
                ? (y - offsetY - (minecraft.font.lineHeight * this.textScale) / 2.0f) / coordinateScale
                : (y - offsetY - 20 * this.textScale) / coordinateScale;

            poseStack.translate(offsetX, offsetY);
            poseStack.scale(coordinateScale, coordinateScale);
            poseStack.translate(adjustedX, adjustedY);
            poseStack.scale(this.textScale / coordinateScale, this.textScale / coordinateScale);
            guiGraphics.centeredText(this.minecraft.font, value.subTitle, 0, 0, textAlpha | 0xfdfdfd);
            poseStack.popMatrix();
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
        float rangeAngle = this.normalizePositiveAngle(currentSection.angleEnd - currentSection.angleStart) / 2.0f;
        this.renderAnnularSectorSelection(
            guiGraphics,
            this.centerPos.x,
            this.centerPos.y,
            this.selectionEffectColor,
            this.ringInnerRadius * 2,
            this.ringOuterRadius * 2,
            currentSection.angle,
            rangeAngle
        );
    }

    private float normalizePositiveAngle(float angle) {
        float normalized = angle % TAU;
        return normalized < 0 ? normalized + TAU : normalized;
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
