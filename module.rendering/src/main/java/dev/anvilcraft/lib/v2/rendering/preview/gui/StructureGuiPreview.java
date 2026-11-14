package dev.anvilcraft.lib.v2.rendering.preview.gui;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

/** 使用屏幕坐标的只读界面浮窗，调用方提供动画偏好。 */
@Slf4j
public final class StructureGuiPreview {
    private final BooleanSupplier reducedMotion;

    public StructureGuiPreview() {
        this(() -> false);
    }

    public StructureGuiPreview(BooleanSupplier reducedMotion) {
        this.reducedMotion = java.util.Objects.requireNonNull(reducedMotion);
    }

    private @Nullable StructureGuiSession session;
    private @Nullable Object owner;
    private BlockPos block = BlockPos.ZERO;
    private float anchorX;
    private float anchorY;
    private boolean anchorVisible;
    private float x;
    private float y;
    private float scale = 1;
    private float progress;
    private float transitionStart;
    private float target = 1;
    private long transitionTime;
    private boolean dragging;
    private int capturedButton = -1;
    private float dragOffsetX;
    private float dragOffsetY;

    public boolean open(Object owner, StructureTemplate template, BlockPos pos, float anchorX, float anchorY) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            StructureGuiSession next = StructureGuiSession.create(minecraft, template, pos);
            if (next == null) {
                return false;
            }
            this.session = next;
            this.owner = owner;
            this.block = pos.immutable();
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorVisible = true;
            this.scale = Math.min(1, Math.min((screenWidth() - 16f) / next.screen().getXSize(),
                (screenHeight() - 16f) / next.screen().getYSize()));
            this.x = anchorX + 24;
            this.y = anchorY - this.height() / 2;
            if (this.x + this.width() > screenWidth() - 4) {
                this.x = anchorX - 24 - this.width();
            }
            this.constrain();
            this.progress = 0;
            this.transitionTime = 0;
            this.dragging = false;
            this.retarget(1);
            return true;
        } catch (RuntimeException exception) {
            log.warn("Cannot preview structure menu at {}", pos, exception);
            return false;
        }
    }

    public @Nullable BlockPos blockFor(Object component) {
        return this.owner == component && this.session != null ? this.block : null;
    }

    public void beginFrame() {
        this.anchorVisible = false;
    }

    public void anchor(float x, float y, boolean visible) {
        this.anchorX = x;
        this.anchorY = y;
        this.anchorVisible = visible;
    }

    public boolean contains(double mouseX, double mouseY) {
        return this.session != null && mouseX >= this.x && mouseX < this.x + this.width()
            && mouseY >= this.y && mouseY < this.y + this.height();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.contains(mouseX, mouseY)) {
            return false;
        }
        this.capturedButton = button;
        if (button == 1) {
            this.dragging = false;
            this.retarget(0);
        } else if (button == 0 && this.target != 0) {
            this.dragging = true;
            this.dragOffsetX = (float) mouseX - this.x;
            this.dragOffsetY = (float) mouseY - this.y;
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (this.capturedButton != button) {
            return false;
        }
        if (this.dragging) {
            this.x = (float) mouseX - this.dragOffsetX;
            this.y = (float) mouseY - this.dragOffsetY;
            this.constrain();
        }
        return true;
    }

    public boolean mouseReleased(int button) {
        if (this.capturedButton != button) {
            return false;
        }
        this.dragging = false;
        this.capturedButton = -1;
        return true;
    }

    public void clear() {
        // 原生 onClose/removed 可能发包或归还槽位物品；预览从不调用这些交互生命周期。
        this.session = null;
        this.owner = null;
        this.dragging = false;
        this.capturedButton = -1;
    }

    public void render(GuiGraphics graphics) {
        if (this.session == null) {
            return;
        }
        this.updateProgress();
        if (this.target == 0 && this.progress <= 0.001f) {
            this.clear();
            return;
        }
        this.constrain();
        float motion = this.reducedMotion.getAsBoolean() ? 1 : 0.95f + this.progress * 0.05f;
        float width = this.width() * motion, height = this.height() * motion;
        float x = this.x + (this.width() - width) / 2, y = this.y + (this.height() - height) / 2;
        int color = ((int) (255 * this.progress) << 24) | 0xFFFFFF;
        graphics.nextStratum();
        if (this.anchorVisible) {
            Vector2f center = new Vector2f(x + width / 2, y + height / 2);
            drawLine(graphics, this.anchorX, this.anchorY, center.x, center.y, color);
            if (center.distanceSquared(this.anchorX, this.anchorY) > 0.1f) {
                // 半径 2 的圆点，比 1 像素连线更醒目。
                for (int row = -2; row <= 2; row++) {
                    int half = row == -2 || row == 2 ? 1 : 2;
                    graphics.fill(Math.round(this.anchorX) - half, Math.round(this.anchorY) + row,
                        Math.round(this.anchorX) + half + 1, Math.round(this.anchorY) + row + 1, color);
                }
            }
        }
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(this.scale * motion);
        var screen = this.session.screen();
        graphics.enableScissor(0, 0, screen.getXSize(), screen.getYSize());
        try {
            StructureGuiSession.renderOnly(this.progress, () -> {
                screen.renderBackground(graphics, -100000, -100000, 0);
                screen.render(graphics, -100000, -100000, 0);
            });
        } catch (RuntimeException exception) {
            log.warn("Cannot render read-only structure menu", exception);
            this.clear();
        } finally {
            graphics.disableScissor();
            graphics.pose().popMatrix();
        }
    }

    private static void drawLine(GuiGraphics graphics, float x0, float y0, float x1, float y1, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.1f) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x0, y0);
        graphics.pose().rotate((float) Math.atan2(dy, dx));
        graphics.fill(0, 0, Math.round(length), 1, color);
        graphics.pose().popMatrix();
    }

    private void retarget(float target) {
        this.updateProgress();
        this.transitionStart = this.progress;
        this.target = target;
        this.transitionTime = System.nanoTime();
    }

    private void updateProgress() {
        if (this.transitionTime == 0) {
            return;
        }
        float elapsed = (System.nanoTime() - this.transitionTime) / 200_000_000f;
        this.progress = this.transitionStart + (this.target - this.transitionStart) * StructureGuiGeometry.easeOut(elapsed);
    }

    private void constrain() {
        this.x = Math.clamp(this.x, 0, Math.max(0, screenWidth() - this.width()));
        this.y = Math.clamp(this.y, 0, Math.max(0, screenHeight() - this.height()));
    }

    private float width() { return this.session == null ? 0 : this.session.screen().getXSize() * this.scale; }
    private float height() { return this.session == null ? 0 : this.session.screen().getYSize() * this.scale; }
    private static int screenWidth() { return Minecraft.getInstance().getWindow().getGuiScaledWidth(); }
    private static int screenHeight() { return Minecraft.getInstance().getWindow().getGuiScaledHeight(); }
}
