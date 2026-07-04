package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 下拉菜单。点击展开选项列表，选择后收起。
 * 弹出层延迟渲染以确保 z-order 正确，支持最大高度 + 滚动。
 */
@Accessors(fluent = true)
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class DropdownComponent implements UIComponent {
    private static final int BG_COLOR = 0xFF404040;
    private static final int HOVER_COLOR = 0xFF606060;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int POPUP_BG = 0xFF303030;
    private static final int POPUP_HOVER = 0xFF505050;
    private static final int SCROLLBAR_COLOR = 0xFF888888;
    private static final int SCROLLBAR_BG = 0xFF222222;
    private static final int SCROLLBAR_W = 4;
    private static final float PADDING_H = 8;
    private static final float PADDING_V = 4;
    private static final float ARROW_SIZE = 6;
    private final String[] options;
    private final float maxPopupHeight;
    @Getter
    @Setter
    private Modifier modifier;
    private int selectedIndex;
    @Getter
    private boolean open;
    private boolean hovered;
    @Getter
    private float popupScrollY;
    @Setter
    private @Nullable Consumer<String> onChange;

    // popup 拖拽
    @Getter
    private boolean scrollbarDragging;
    private float dragAnchorY;

    @Getter
    private float x, y, width, height;

    public DropdownComponent(Modifier modifier, String[] options, int selectedIndex, float maxPopupHeight) {
        this.modifier = modifier;
        this.options = options;
        this.selectedIndex = Mth.clamp(selectedIndex, 0, Math.max(0, options.length - 1));
        this.maxPopupHeight = maxPopupHeight;
    }


    public String selectedOption() {
        return this.options.length > 0 ? this.options[this.selectedIndex] : "";
    }

    public void setOpen(boolean open) {
        this.open = open;
        if (!this.open) this.popupScrollY = 0;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void setPopupScrollY(float y) {
        this.popupScrollY = y;
    }

    @Override
    public List<UIComponent> children() {
        return Collections.emptyList();
    }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        float maxTextW = 0;
        for (String opt : this.options) maxTextW = Math.max(maxTextW, font.width(opt));
        float w = maxTextW + DropdownComponent.PADDING_H * 2 + DropdownComponent.ARROW_SIZE + 8;
        float h = font.lineHeight + DropdownComponent.PADDING_V * 2;
        return MeasuredSize.of(constraints.constrainWidth(w), constraints.constrainHeight(h));
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        this.renderTrigger(extractor);
        this.renderPopup(extractor);
    }

    // ── 触发器渲染 ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) return false;
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (this.isOnPopupScrollbar(mx, my)) {
            this.startPopupScrollbarDrag(my);
            return true;
        }
        if (this.clickPopup(mx, my)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        if (this.clickTrigger(mx, my)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() != 0) return false;
        if (!this.scrollbarDragging()) return false;
        var mc = Minecraft.getInstance();
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        this.onPopupScrollbarDrag(my);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() != 0) return false;
        this.stopPopupScrollbarDrag();
        return false;
    }

    // ── 弹出层渲染（延迟调用，确保 z-order） ──

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.open() && this.popupRect().contains((float) mouseX, (float) mouseY)) {
            return this.onPopupScroll((float) scrollY);
        }
        return false;
    }

    @Override
    public void updateHover(float mx, float my) {
        this.hovered = this.hitRect().contains(mx, my);
    }

    @Override
    public int renderingPriority() {
        return this.open ? 100 : 0;
    }

    @Override
    public int eventPriority() {
        return this.open ? 100 : 0;
    }

    private void renderTrigger(GuiGraphicsExtractor extractor) {
        var font = Minecraft.getInstance().font;
        int ix = (int) this.x, iy = (int) this.y, iw = (int) this.width, ih = (int) this.height;
        int bg = (this.open || this.hovered) ? DropdownComponent.HOVER_COLOR : DropdownComponent.BG_COLOR;
        extractor.fill(ix, iy, ix + iw, iy + ih, bg);

        String label = this.options.length > 0 ? this.options[this.selectedIndex] : "";
        int textY = (int) (this.y + (this.height + font.lineHeight) / 2f - font.lineHeight);
        extractor.text(font, label, ix + (int) DropdownComponent.PADDING_H, textY, DropdownComponent.TEXT_COLOR);

        // SDF 三角箭头
        float arrowCx = this.x + this.width - DropdownComponent.PADDING_H - DropdownComponent.ARROW_SIZE / 2f;
        float arrowCy = this.y + this.height / 2f;
        float hs = DropdownComponent.ARROW_SIZE / 2f;
        if (this.open) {
            SdfGraphics.instance
                .reset()
                .triangle(
                    arrowCx - hs, arrowCy + hs * 0.6f,
                    arrowCx + hs, arrowCy + hs * 0.6f,
                    arrowCx, arrowCy - hs * 0.8f
                )
                .color(DropdownComponent.TEXT_COLOR)
                .fill()
                .draw(extractor);
        } else {
            SdfGraphics.instance
                .reset()
                .triangle(
                    arrowCx - hs, arrowCy - hs * 0.6f,
                    arrowCx + hs, arrowCy - hs * 0.6f,
                    arrowCx, arrowCy + hs * 0.8f
                )
                .color(DropdownComponent.TEXT_COLOR)
                .fill()
                .draw(extractor);
        }
    }

    // ── 交互 ──

    /**
     * 弹出层 item 高度。
     */
    public float itemHeight() {
        return Minecraft.getInstance().font.lineHeight + 4;
    }

    /**
     * 实际弹出层高度（min 内容高度, maxPopupHeight）。
     */
    public float popupHeight() {
        if (this.options.length == 0) return 0;
        return Math.min(this.options.length * this.itemHeight(), this.maxPopupHeight);
    }

    /**
     * 弹出层包围盒。
     */
    public LayoutRect popupRect() {
        float ph = this.popupHeight();
        return LayoutRect.of(this.x, this.y + this.height, this.width, ph);
    }

    /**
     * 延迟渲染弹出层（在所有组件之后调用）。
     */
    public void renderPopup(GuiGraphicsExtractor extractor) {
        if (!this.open || this.options.length == 0) return;

        var font = Minecraft.getInstance().font;
        float itemH = this.itemHeight();
        float ph = this.popupHeight();
        float totalH = this.options.length * itemH;
        float maxScroll = Math.max(0, totalH - ph);
        this.popupScrollY = Mth.clamp(this.popupScrollY, -maxScroll, 0);

        int px = (int) this.x, py = (int) (this.y + this.height), pw = (int) this.width;

        extractor.enableScissor(px, py, px + pw, py + (int) ph);
        extractor.fill(px, py, px + pw, py + (int) ph, DropdownComponent.POPUP_BG);

        // 计算鼠标悬停项
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        int hoveredIdx = -1;
        if (mx >= px && mx < px + pw && my >= py && my < py + (int) ph) {
            hoveredIdx = (int) ((my - py - this.popupScrollY) / itemH);
        }

        float startY = this.y + this.height + this.popupScrollY;
        for (int i = 0; i < this.options.length; i++) {
            float iy = startY + i * itemH;
            float bottom = iy + itemH;
            if (bottom <= this.y + this.height || iy >= this.y + this.height + ph) continue;

            int bg;
            if (i == this.selectedIndex) {
                bg = DropdownComponent.POPUP_HOVER;
            } else if (i == hoveredIdx) {
                bg = DropdownComponent.SCROLLBAR_COLOR; // 悬停高亮
            } else {
                bg = DropdownComponent.POPUP_BG;
            }
            int fillTop = Math.max((int) iy, py);
            int fillBot = Math.min((int) bottom, py + (int) ph);
            extractor.fill(px, fillTop, px + pw, fillBot, bg);
            extractor.text(font, this.options[i], px + (int) DropdownComponent.PADDING_H, (int) iy + 2, DropdownComponent.TEXT_COLOR);
        }
        extractor.disableScissor();

        // 滚动条
        if (totalH > ph) {
            float bh = Math.max(12, ph * ph / totalH);
            float by = py + (-this.popupScrollY / maxScroll) * (ph - bh);
            int bx = px + pw - DropdownComponent.SCROLLBAR_W - 1;
            extractor.fill(bx, py, bx + DropdownComponent.SCROLLBAR_W, py + (int) ph, DropdownComponent.SCROLLBAR_BG);
            extractor.fill(bx, (int) by, bx + DropdownComponent.SCROLLBAR_W, (int) (by + bh), DropdownComponent.SCROLLBAR_COLOR);
        }
    }

    /**
     * 点击触发器区域 → 切换展开。
     */
    public boolean clickTrigger(float px, float py) {
        if (this.triggerRect().contains(px, py)) {
            this.open = !this.open;
            if (!this.open) this.popupScrollY = 0;
            return true;
        }
        return false;
    }

    /**
     * 点击弹出层选项 → 选中并收起。返回 true 表示命中。
     */
    public boolean clickPopup(float px, float py) {
        if (!this.open || this.options.length == 0) return false;
        float ph = this.popupHeight();
        if (px < this.x || px > this.x + this.width || py < this.y + this.height || py > this.y + this.height + ph) return false;

        float itemH = this.itemHeight();
        float idxF = (py - this.y - this.height - this.popupScrollY) / itemH;
        int idx = (int) idxF;
        if (idx >= 0 && idx < this.options.length) {
            this.select(idx);
            return true;
        }
        return false;
    }

    /**
     * 滚轮滚动弹出层。
     */
    public boolean onPopupScroll(float amount) {
        if (!this.open) return false;
        float ph = this.popupHeight();
        float totalH = this.options.length * this.itemHeight();
        if (totalH <= ph) return false;
        float maxScroll = totalH - ph;
        this.popupScrollY = Mth.clamp(this.popupScrollY + amount * 20, -maxScroll, 0);
        return true;
    }

    /**
     * 弹出层滚动条命中测试。
     */
    public boolean isOnPopupScrollbar(float mx, float my) {
        if (!this.open) return false;
        float ph = this.popupHeight();
        int py = (int) (this.y + this.height);
        float totalH = this.options.length * this.itemHeight();
        if (totalH <= ph) return false;
        int bx = (int) (this.x + this.width - DropdownComponent.SCROLLBAR_W - 1);
        return mx >= bx && mx < bx + DropdownComponent.SCROLLBAR_W && my >= py && my < py + ph;
    }

    public void startPopupScrollbarDrag(float my) {
        this.scrollbarDragging = true;
        float ph = this.popupHeight();
        int py = (int) (this.y + this.height);
        float totalH = this.options.length * this.itemHeight();
        float bh = Math.max(12, ph * ph / totalH);
        float maxScroll = totalH - ph;
        float by = py + (-this.popupScrollY / maxScroll) * (ph - bh);
        if (my >= by && my < by + bh) {
            this.dragAnchorY = my - by;
        } else {
            this.dragAnchorY = bh / 2f;
            float newBarY = my - this.dragAnchorY;
            float ratio = Mth.clamp((newBarY - py) / (ph - bh), 0f, 1f);
            this.popupScrollY = -(ratio * maxScroll);
        }
    }

    public void onPopupScrollbarDrag(float my) {
        if (!this.scrollbarDragging) return;
        float ph = this.popupHeight();
        int py = (int) (this.y + this.height);
        float totalH = this.options.length * this.itemHeight();
        float bh = Math.max(12, ph * ph / totalH);
        float maxScroll = totalH - ph;
        float newBarY = my - this.dragAnchorY;
        float ratio = Mth.clamp((newBarY - py) / (ph - bh), 0f, 1f);
        this.popupScrollY = -(ratio * maxScroll);
    }

    public void stopPopupScrollbarDrag() {
        this.scrollbarDragging = false;
    }

    private void select(int idx) {
        if (idx != this.selectedIndex) {
            this.selectedIndex = idx;
            if (this.onChange != null) this.onChange.accept(this.options[idx]);
        }
        this.open = false;
        this.popupScrollY = 0;
    }

    private LayoutRect triggerRect() {
        return LayoutRect.of(this.x, this.y, this.width, this.height);
    }

    public LayoutRect hitRect() {
        return this.triggerRect();
    }

    @Override
    public void copyRuntimeState(UIComponent old) {
        if (old instanceof DropdownComponent oldDd) {
            this.setOpen(oldDd.open());
            this.setPopupScrollY(oldDd.popupScrollY());
            this.selectedIndex = oldDd.selectedIndex;
            this.scrollbarDragging = oldDd.scrollbarDragging;
            this.dragAnchorY = oldDd.dragAnchorY;
        }
    }
}
