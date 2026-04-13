package dev.anvilcraft.lib.v2.wheel.client.gui.component;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.wheel.client.init.LibShaders;
import dev.anvilcraft.lib.v2.wheel.util.MathUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WheelWidget extends AbstractWidget {
    public static final int IGNORE_CURSOR_MOVE_LENGTH = 15;
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
    private final int selectionEffectColor;
    private final int selectionEffectRadius;
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

    public WheelWidget(
        int x, int y, int width, int height,
        float ringInnerRadius, float ringOuterRadius, float textScale,
        List<RawSection> sections
    ) {
        this(x, y, width, height, Component.empty(), ringInnerRadius, ringOuterRadius, textScale, sections);
    }

    public WheelWidget(
        int x, int y, int width, int height,
        float ringInnerRadius, float ringOuterRadius, float textScale, float degreeOffsetAngle,
        List<RawSection> sections
    ) {
        this(x, y, width, height, Component.empty(), ringInnerRadius, ringOuterRadius, textScale, degreeOffsetAngle, sections);
    }

    public WheelWidget(
        int x, int y, int width, int height,
        float ringInnerRadius, float ringOuterRadius,
        List<RawSection> sections
    ) {
        this(x, y, width, height, Component.empty(), ringInnerRadius, ringOuterRadius, sections);
    }

    public WheelWidget(
        int x, int y, int width, int height, Component message,
        float ringInnerRadius, float ringOuterRadius, float textScale, float degreeOffsetAngle,
        List<RawSection> sections
    ) {
        this(
            x, y, width, height, message,
            ringInnerRadius, ringOuterRadius,
            150, 300, 150,
            0x88000000,
            0xddffff00, 20, 5f,
            0xfdfdfd, textScale, degreeOffsetAngle,
            sections
        );
    }

    public WheelWidget(
        int x, int y, int width, int height, Component message,
        float ringInnerRadius, float ringOuterRadius,
        List<RawSection> sections
    ) {
        this(
            x, y, width, height, message,
            ringInnerRadius, ringOuterRadius,
            150, 300, 150,
            0x88000000,
            0xddffff00, 20, 5f,
            0xfdfdfd, 1f, 0f,
            sections
        );
    }

    public WheelWidget(
        int x, int y, int width, int height, Component message,
        float ringInnerRadius, float ringOuterRadius, float degreeOffsetAngle,
        List<RawSection> sections
    ) {
        this(
            x, y, width, height, message,
            ringInnerRadius, ringOuterRadius,
            150, 300, 150,
            0x88000000,
            0xddffff00, 20, 5f,
            0xfdfdfd, 1f, degreeOffsetAngle,
            sections
        );
    }

    public WheelWidget(
        int x, int y, int width, int height,
        float ringInnerRadius, float ringOuterRadius,
        int delay, int animationMs, int closingAnimationMs,
        int ringColor,
        int selectionEffectColor, int selectionEffectRadius, float selectionAnimationSpeedFactor,
        int textColor, float textScale,
        List<RawSection> sections
    ) {
        this(
            x, y, width, height, Component.empty(),
            ringInnerRadius, ringOuterRadius,
            delay, animationMs, closingAnimationMs,
            ringColor,
            selectionEffectColor, selectionEffectRadius, selectionAnimationSpeedFactor,
            textColor, textScale, 0f,
            sections
        );
    }

    public WheelWidget(
        int x, int y, int width, int height, Component message,
        float ringInnerRadius, float ringOuterRadius,
        int delay, int animationMs, int closingAnimationMs,
        int ringColor,
        int selectionEffectColor, int selectionEffectRadius, float selectionAnimationSpeedFactor,
        int textColor, float textScale, float degreeOffsetAngle,
        List<RawSection> sections
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
        this.selectionEffectRadius = selectionEffectRadius;
        this.selectionAnimationSpeedFactor = selectionAnimationSpeedFactor;
        this.textColor = textColor;
        this.textScale = textScale;
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
        this.selectionEffectPos = MathUtil.rotate(
            MathUtil.copy(ROTATION_START)
                .mul(this.getSectionCircleDiameter()),
            this.currentAngle
        );
    }

    public WheelWidget setCurrentIndex(int index) {
        if (index < 0 || index >= this.sections.size()) return this;
        if (!this.sections.get(index).selectable()) return this;
        this.currentSectionIndex = index;
        this.currentAngle = this.sections.get(index).angle;
        this.selectionEffectPos = MathUtil.rotate(
            MathUtil.copy(ROTATION_START)
                .mul(this.getSectionCircleDiameter()),
            this.currentAngle
        );
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
        if (cursorPos.length() < IGNORE_CURSOR_MOVE_LENGTH) return;
        Vector2f rotationStart = new Vector2f(0, 1);
        cursorPos.normalize();
        double rot = Math.acos(rotationStart.dot(cursorPos) / (rotationStart.length() * cursorPos.length()));
        double rotation = cursorPos.x < 0 ? Math.PI - rot : Math.PI + rot;
        for (WheelSection section : this.sections) {
            if ((section.angleStart > section.angleEnd && rotation >= section.angleStart
                || rotation >= section.angleStart && rotation <= section.angleEnd)
                && section.selectable
            ) {
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.checkMousePos(mouseX, mouseY);
        this.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
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
        WheelWidget.renderRing(
            guiGraphics,
            this.centerPos.x,
            this.centerPos.y,
            this.ringColor,
            this.ringInnerRadius * 2,
            this.ringOuterRadius * 2
        );
        this.renderSelection(guiGraphics);
        for (WheelSection value : this.sections) {
            float x = value.center.x;
            float y = value.center.y;
            poseStack.pushPose();
            poseStack.translate(x - 10, y - 10, 100);
            value.renderer().render(guiGraphics, poseStack, 20, 20);
            poseStack.popPose();
            poseStack.pushPose();
            float coordinateScale = 0.7f;
            float offsetX = 0.1f * this.width;
            float offsetY = 0.1f * this.height;
            float adjustedX = (x - offsetX) / coordinateScale;
            float adjustedY = (y - offsetY - 20 * this.textScale) / coordinateScale;

            poseStack.translate(offsetX, offsetY, 0);
            poseStack.scale(coordinateScale, coordinateScale, coordinateScale);
            poseStack.translate(adjustedX, adjustedY, 0);
            poseStack.scale(this.textScale / coordinateScale, this.textScale / coordinateScale, this.textScale / coordinateScale);
            guiGraphics.drawCenteredString(
                minecraft.font,
                value.subTitle,
                0,
                0,
                (0xff << 24) | this.textColor
            );
            poseStack.popPose();
        }
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
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
        WheelWidget.renderRing(
            guiGraphics,
            this.centerPos.x,
            this.centerPos.y,
            this.ringColor,
            this.ringInnerRadius * 2 * progress,
            this.ringOuterRadius * 2 * progress
        );
        poseStack.popPose();
        if (this.currentSectionIndex != -1) {
            WheelSection section = this.sections.get(this.currentSectionIndex);
            Vector2f center = new Vector2f(
                (section.center.x - this.centerPos.x) / this.getSectionCircleDiameter(),
                (section.center.y - this.centerPos.y) / this.getSectionCircleDiameter()
            ).mul(this.getSectionCircleDiameter() * progress).add(this.centerPos.x, this.centerPos.y);
            WheelWidget.renderSelectionEffect(
                guiGraphics,
                center.x,
                center.y,
                this.selectionEffectColor,
                this.selectionEffectRadius
            );
        }
        for (WheelSection value : this.sections) {
            Vector2f center = new Vector2f(
                (value.center.x - this.centerPos.x) / this.getSectionCircleDiameter(),
                (value.center.y - this.centerPos.y) / this.getSectionCircleDiameter()
            ).mul(this.getSectionCircleDiameter() * progress).add(this.centerPos.x, this.centerPos.y);
            float x = center.x;
            float y = center.y;
            poseStack.pushPose();
            poseStack.translate(x - 10, y - 10, 100);
            value.renderer().render(guiGraphics, poseStack, 20, 20);
            poseStack.popPose();
            final int textAlpha = (int) (progress * 0xff) << 24;
            poseStack.pushPose();
            float coordinateScale = 0.7f;
            float offsetX = 0.1f * this.width;
            float offsetY = 0.1f * this.height;
            float adjustedX = (x - offsetX) / coordinateScale;
            float adjustedY = (y - offsetY - 20 * this.textScale) / coordinateScale;

            poseStack.translate(offsetX, offsetY, 0);
            poseStack.scale(coordinateScale, coordinateScale, coordinateScale);
            poseStack.translate(adjustedX, adjustedY, 0);
            poseStack.scale(this.textScale / coordinateScale, this.textScale / coordinateScale, this.textScale / coordinateScale);
            guiGraphics.drawCenteredString(
                this.minecraft.font,
                value.subTitle,
                0,
                0,
                textAlpha | 0xfdfdfd
            );
            poseStack.popPose();
        }
    }

    private void renderSelection(GuiGraphics guiGraphics) {
        float selectionEffectAngle = MathUtil.angle(
            MathUtil.copy(ROTATION_START),
            this.selectionEffectPos
        );

        float diffAngle = this.currentAngle - selectionEffectAngle;

        if (diffAngle > Math.PI) {
            diffAngle -= (float) (Math.PI * 2);
        } else if (diffAngle < -Math.PI) {
            diffAngle += (float) (Math.PI * 2);
        }

        this.selectionEffectPos = MathUtil.rotate(
            this.selectionEffectPos,
            diffAngle / this.selectionAnimationSpeedFactor
        );

        Vector2f pos = MathUtil.copy(this.selectionEffectPos)
            .mul(1, -1)
            .add(this.centerPos);

        WheelWidget.renderSelectionEffect(
            guiGraphics,
            pos.x,
            pos.y,
            this.selectionEffectColor,
            this.selectionEffectRadius
        );
    }

    public void onClosing() {
        if (this.shouldRender() && !this.closingAnimationStarted) {
            this.displayTime = System.currentTimeMillis();
            this.closingAnimationStarted = true;
        } else {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public record WheelSection(
        Vector2f center,
        float angle,
        float angleStart,
        float angleEnd,
        Component subTitle,
        SectionRenderer renderer,
        boolean selectable
    ) {
        public WheelSection(
            Vector2f center,
            float angle,
            float angleStart,
            float angleEnd,
            RawSection section
        ) {
            this(center, angle, angleStart, angleEnd, section.name(), section.renderer(), section.selectable());
        }
    }

    public record RawSection(Component name, SectionRenderer renderer, boolean selectable) {
        public RawSection(Component name, SectionRenderer renderer) {
            this(name, renderer, true);
        }
    }

    @FunctionalInterface
    public interface SectionRenderer {
        void render(GuiGraphics graphics, PoseStack pose, int width, int height);
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
        ShaderInstance ringShader = LibShaders.getRingShader();
        if (ringShader == null) {
            renderRingFallback(guiGraphics, centerX, centerY, color, innerDiameter, outerDiameter);
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
        RenderSystem.enableDepthTest();
    }

    public static void renderSelectionEffect(
        GuiGraphics guiGraphics,
        float centerX,
        float centerY,
        int color,
        float radius
    ) {
        PoseStack poseStack = guiGraphics.pose();
        Matrix4f matrix4f = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_COLOR
        );
        float x1 = centerX - radius - 5;
        float y1 = centerY - radius - 5;
        float x2 = centerX + radius + 5;
        float y2 = centerY + radius + 5;
        bufferBuilder.addVertex(matrix4f, x1, y1, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x1, y2, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y2, SELECTION_Z).setColor(color);
        bufferBuilder.addVertex(matrix4f, x2, y1, SELECTION_Z).setColor(color);

        Window window = Minecraft.getInstance().getWindow();
        float guiScale = (float) window.getGuiScale();
        RenderSystem.disableDepthTest();
        ShaderInstance selectionShader = LibShaders.getSelectionShader();
        if (selectionShader == null) {
            renderSelectionFallback(guiGraphics, centerX, centerY, color, radius);
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
            .set(radius * guiScale);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
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

    private static void renderSelectionFallback(
        GuiGraphics guiGraphics,
        float centerX,
        float centerY,
        int color,
        float radius
    ) {
        renderDisc(guiGraphics, centerX, centerY, radius, color);
    }

    private static void renderDisc(
        GuiGraphics guiGraphics,
        float centerX,
        float centerY,
        float radius,
        int centerColor
    ) {
        PoseStack poseStack = guiGraphics.pose();
        Matrix4f matrix4f = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(
            VertexFormat.Mode.TRIANGLE_FAN,
            DefaultVertexFormat.POSITION_COLOR
        );
        int edgeColor = centerColor & 0x00FFFFFF;
        int segments = 48;
        bufferBuilder.addVertex(matrix4f, centerX, centerY, SELECTION_Z).setColor(centerColor);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 2 * i / segments);
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            bufferBuilder.addVertex(matrix4f, centerX + cos * radius, centerY + sin * radius, SELECTION_Z).setColor(edgeColor);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
    }
}
