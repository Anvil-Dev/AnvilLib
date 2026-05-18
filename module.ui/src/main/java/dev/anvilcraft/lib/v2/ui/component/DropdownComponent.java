package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 下拉菜单。点击展开选项列表，选择后收起。
 * 弹出层超出组件边界绘制，支持滚动（最多显示 6 项）。
 */
public class DropdownComponent implements UIComponent {

    private static final int BG_COLOR       = 0xFF404040;
    private static final int HOVER_COLOR    = 0xFF606060;
    private static final int TEXT_COLOR     = 0xFFFFFFFF;
    private static final int POPUP_BG       = 0xFF303030;
    private static final int POPUP_HOVER    = 0xFF505050;
    private static final float PADDING_H    = 8;
    private static final float PADDING_V    = 4;
    private static final float ARROW_W      = 10;
    private static final int MAX_VISIBLE    = 6;

    private Modifier modifier;
    private final String[] options;
    private int selectedIndex;
    private boolean open;
    private Consumer<String> onChange;

    private float x, y, width, height;

    public DropdownComponent(Modifier modifier, String[] options, int selectedIndex) {
        this.modifier = modifier;
        this.options = options;
        this.selectedIndex = Mth.clamp(selectedIndex, 0, options.length - 1);
    }

    public DropdownComponent modifier(Modifier m) { this.modifier = m; return this; }
    public DropdownComponent onChange(Consumer<String> onChange) { this.onChange = onChange; return this; }

    public String selectedOption() { return options[selectedIndex]; }
    public int selectedIndex() { return selectedIndex; }

    public void setOpen(boolean open) { this.open = open; }
    public boolean isOpen() { return open; }

    @Override public Modifier modifier() { return modifier; }
    @Override public List<UIComponent> children() { return Collections.emptyList(); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        float maxTextW = 0;
        for (String opt : options) maxTextW = Math.max(maxTextW, font.width(opt));
        float w = maxTextW + PADDING_H * 2 + ARROW_W;
        float h = font.lineHeight + PADDING_V * 2;
        return MeasuredSize.of(constraints.constrainWidth(w), constraints.constrainHeight(h));
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        var font = Minecraft.getInstance().font;
        int ix = (int) x, iy = (int) y, iw = (int) width, ih = (int) height;

        // 触发器
        int bg = open ? HOVER_COLOR : BG_COLOR;
        extractor.fill(ix, iy, ix + iw, iy + ih, bg);

        String label = options[selectedIndex];
        int textY = (int) (y + (height + font.lineHeight) / 2f - font.lineHeight);
        extractor.text(font, label, ix + (int) PADDING_H, textY, TEXT_COLOR);

        // ▼ 箭头
        String arrow = open ? "▲" : "▼";
        float arrowW = font.width(arrow);
        extractor.text(font, arrow, (int) (x + width - PADDING_H - arrowW), textY, TEXT_COLOR);

        // 弹出层
        if (open && options.length > 0) {
            int visible = Math.min(options.length, MAX_VISIBLE);
            float itemH = font.lineHeight + 4;
            float popupH = itemH * visible;
            int piy = iy + ih;

            extractor.enableScissor(ix, piy, ix + iw, piy + (int) popupH);
            extractor.fill(ix, piy, ix + iw, piy + (int) popupH, POPUP_BG);

            for (int i = 0; i < options.length; i++) {
                float itemY = piy + i * itemH;
                if (i < visible) {
                    int itemBg = (i == selectedIndex) ? HOVER_COLOR : POPUP_BG;
                    extractor.fill(ix, (int) itemY, ix + iw, (int) (itemY + itemH), itemBg);
                    extractor.text(font, options[i], ix + (int) PADDING_H, (int) (itemY + 2), TEXT_COLOR);
                }
            }
            extractor.disableScissor();
        }
    }

    /** 点击触发器区域 → 切换展开。返回 true 表示已消费事件。 */
    public boolean clickTrigger(float px, float py) {
        if (triggerRect().contains(px, py)) {
            open = !open;
            return true;
        }
        return false;
    }

    /** 点击弹出层中的某一项 → 选中并收起。返回 true 表示命中。 */
    public boolean clickPopup(float px, float py) {
        if (!open) return false;
        var font = Minecraft.getInstance().font;
        float itemH = font.lineHeight + 4;
        int visible = Math.min(options.length, MAX_VISIBLE);
        float popupH = itemH * visible;
        if (px < x || px > x + width || py < y + height || py > y + height + popupH) return false;

        int idx = (int) ((py - y - height) / itemH);
        if (idx >= 0 && idx < options.length && idx < visible) {
            select(idx);
            return true;
        }
        return false;
    }

    private void select(int idx) {
        if (idx != selectedIndex) {
            selectedIndex = idx;
            if (onChange != null) onChange.accept(options[idx]);
        }
        open = false;
    }

    private LayoutRect triggerRect() {
        return LayoutRect.of(x, y, width, height);
    }

    public LayoutRect hitRect() {
        if (open) {
            var font = Minecraft.getInstance().font;
            float itemH = font.lineHeight + 4;
            int visible = Math.min(options.length, MAX_VISIBLE);
            return LayoutRect.of(x, y, width, height + itemH * visible);
        }
        return triggerRect();
    }
}
