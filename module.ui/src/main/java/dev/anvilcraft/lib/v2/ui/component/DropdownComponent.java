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
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 下拉菜单。点击展开选项列表，选择后收起。
 * 弹出层延迟渲染以确保 z-order 正确，支持最大高度 + 滚动。
 */
@Accessors(fluent = true)
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
    @Getter
    private float popupScrollY;
    @Setter
    private Consumer<String> onChange;

    // popup 拖拽
    @Getter
    private boolean scrollbarDragging;
    private float dragAnchorY;

    private float x, y, width, height;

    public DropdownComponent(Modifier modifier, String[] options, int selectedIndex, float maxPopupHeight) {
        this.modifier = modifier;
        this.options = options;
        this.selectedIndex = Mth.clamp(selectedIndex, 0, Math.max(0, options.length - 1));
        this.maxPopupHeight = maxPopupHeight;
    }


    public String selectedOption() {
        return options.length > 0 ? options[selectedIndex] : "";
    }

    public void setOpen(boolean open) {
        this.open = open;
        if (!open) popupScrollY = 0;
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
        for (String opt : options) maxTextW = Math.max(maxTextW, font.width(opt));
        float w = maxTextW + PADDING_H * 2 + ARROW_SIZE + 8;
        float h = font.lineHeight + PADDING_V * 2;
        return MeasuredSize.of(constraints.constrainWidth(w), constraints.constrainHeight(h));
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ── 触发器渲染 ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        renderTrigger(extractor);
        // 弹出层由 DeclarativeScreen 在最后统一渲染
    }

    private void renderTrigger(GuiGraphicsExtractor extractor) {
        var font = Minecraft.getInstance().font;
        int ix = (int) x, iy = (int) y, iw = (int) width, ih = (int) height;
        int bg = open ? HOVER_COLOR : BG_COLOR;
        extractor.fill(ix, iy, ix + iw, iy + ih, bg);

        String label = options.length > 0 ? options[selectedIndex] : "";
        int textY = (int) (y + (height + font.lineHeight) / 2f - font.lineHeight);
        extractor.text(font, label, ix + (int) PADDING_H, textY, TEXT_COLOR);

        // SDF 三角箭头
        float arrowCx = x + width - PADDING_H - ARROW_SIZE / 2f;
        float arrowCy = y + height / 2f;
        float hs = ARROW_SIZE / 2f;
        if (open) {
            SdfGraphics.instance
                .triangle(
                    arrowCx - hs, arrowCy + hs * 0.6f,
                    arrowCx + hs, arrowCy + hs * 0.6f,
                    arrowCx, arrowCy - hs * 0.8f
                )
                .color(TEXT_COLOR)
                .fill()
                .draw(extractor);
        } else {
            SdfGraphics.instance
                .triangle(
                    arrowCx - hs, arrowCy - hs * 0.6f,
                    arrowCx + hs, arrowCy - hs * 0.6f,
                    arrowCx, arrowCy + hs * 0.8f
                )
                .color(TEXT_COLOR)
                .fill()
                .draw(extractor);
        }
    }

    // ── 弹出层渲染（延迟调用，确保 z-order） ──

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
        if (options.length == 0) return 0;
        return Math.min(options.length * itemHeight(), maxPopupHeight);
    }

    /**
     * 弹出层包围盒。
     */
    public LayoutRect popupRect() {
        float ph = popupHeight();
        return LayoutRect.of(x, y + height, width, ph);
    }

    /**
     * 延迟渲染弹出层（在所有组件之后调用）。
     */
    public void renderPopup(GuiGraphicsExtractor extractor) {
        if (!open || options.length == 0) return;

        var font = Minecraft.getInstance().font;
        float itemH = itemHeight();
        float ph = popupHeight();
        float totalH = options.length * itemH;
        float maxScroll = Math.max(0, totalH - ph);
        popupScrollY = Mth.clamp(popupScrollY, -maxScroll, 0);

        int px = (int) x, py = (int) (y + height), pw = (int) width;

        extractor.enableScissor(px, py, px + pw, py + (int) ph);
        extractor.fill(px, py, px + pw, py + (int) ph, POPUP_BG);

        float startY = y + height + popupScrollY;
        for (int i = 0; i < options.length; i++) {
            float iy = startY + i * itemH;
            float bottom = iy + itemH;
            // 跳过完全不可见的项
            if (bottom <= y + height || iy >= y + height + ph) continue;

            int bg = (i == selectedIndex) ? POPUP_HOVER : POPUP_BG;
            int fillTop = Math.max((int) iy, py);
            int fillBot = Math.min((int) bottom, py + (int) ph);
            extractor.fill(px, fillTop, px + pw, fillBot, bg);
            extractor.text(font, options[i], px + (int) PADDING_H, (int) iy + 2, TEXT_COLOR);
        }
        extractor.disableScissor();

        // 滚动条
        if (totalH > ph) {
            float bh = Math.max(12, ph * ph / totalH);
            float by = py + (-popupScrollY / maxScroll) * (ph - bh);
            int bx = px + pw - SCROLLBAR_W - 1;
            extractor.fill(bx, py, bx + SCROLLBAR_W, py + (int) ph, SCROLLBAR_BG);
            extractor.fill(bx, (int) by, bx + SCROLLBAR_W, (int) (by + bh), SCROLLBAR_COLOR);
        }
    }

    // ── 交互 ──

    /**
     * 点击触发器区域 → 切换展开。
     */
    public boolean clickTrigger(float px, float py) {
        if (triggerRect().contains(px, py)) {
            open = !open;
            if (!open) popupScrollY = 0;
            return true;
        }
        return false;
    }

    /**
     * 点击弹出层选项 → 选中并收起。返回 true 表示命中。
     */
    public boolean clickPopup(float px, float py) {
        if (!open || options.length == 0) return false;
        float ph = popupHeight();
        if (px < x || px > x + width || py < y + height || py > y + height + ph) return false;

        float itemH = itemHeight();
        float idxF = (py - y - height - popupScrollY) / itemH;
        int idx = (int) idxF;
        if (idx >= 0 && idx < options.length) {
            select(idx);
            return true;
        }
        return false;
    }

    /**
     * 滚轮滚动弹出层。
     */
    public boolean onPopupScroll(float amount) {
        if (!open) return false;
        float ph = popupHeight();
        float totalH = options.length * itemHeight();
        if (totalH <= ph) return false;
        float maxScroll = totalH - ph;
        popupScrollY = Mth.clamp(popupScrollY + amount * 20, -maxScroll, 0);
        return true;
    }

    /**
     * 弹出层滚动条命中测试。
     */
    public boolean isOnPopupScrollbar(float mx, float my) {
        if (!open) return false;
        float ph = popupHeight();
        float totalH = options.length * itemHeight();
        if (totalH <= ph) return false;
        float bh = Math.max(12, ph * ph / totalH);
        float maxScroll = totalH - ph;
        float by = y + height + (-popupScrollY / maxScroll) * (ph - bh);
        int bx = (int) (x + width - SCROLLBAR_W - 1);
        return mx >= bx && mx < bx + SCROLLBAR_W && my >= by && my < by + bh;
    }

    public void startPopupScrollbarDrag(float my) {
        scrollbarDragging = true;
        float ph = popupHeight();
        float totalH = options.length * itemHeight();
        float bh = Math.max(12, ph * ph / totalH);
        float maxScroll = totalH - ph;
        float by = y + height + (-popupScrollY / maxScroll) * (ph - bh);
        dragAnchorY = my - by;
    }

    public void onPopupScrollbarDrag(float my) {
        if (!scrollbarDragging) return;
        float ph = popupHeight();
        float totalH = options.length * itemHeight();
        float bh = Math.max(12, ph * ph / totalH);
        float maxScroll = totalH - ph;
        float newBarY = my - dragAnchorY;
        float ratio = Mth.clamp(newBarY / (ph - bh), 0f, 1f);
        popupScrollY = -(ratio * maxScroll);
    }

    public void stopPopupScrollbarDrag() {
        scrollbarDragging = false;
    }

    private void select(int idx) {
        if (idx != selectedIndex) {
            selectedIndex = idx;
            if (onChange != null) onChange.accept(options[idx]);
        }
        open = false;
        popupScrollY = 0;
    }

    private LayoutRect triggerRect() {
        return LayoutRect.of(x, y, width, height);
    }

    public LayoutRect hitRect() {
        return triggerRect();
    }
}
