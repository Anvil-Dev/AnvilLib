package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * 可点击按钮。原版 fill() 背景 + 手动居中 text() 文字。
 */
public class ButtonComponent implements UIComponent {

    // 原版按钮配色
    private static final int BG_COLOR       = 0xFF404040;
    private static final int BG_HOVER_COLOR = 0xFF606060;
    private static final int TEXT_COLOR     = 0xFFFFFFFF;
    private static final float PADDING_H    = 12;
    private static final float PADDING_V    = 6;

    private Modifier modifier;
    private String label;
    @Nullable
    private Runnable onClick;
    private boolean hovered;

    private float x, y, width, height;

    public ButtonComponent(Modifier modifier, String label, @Nullable Runnable onClick) {
        this.modifier = modifier;
        this.label = label;
        this.onClick = onClick;
    }

    public ButtonComponent label(String label) { this.label = label; return this; }
    public ButtonComponent onClick(@Nullable Runnable onClick) { this.onClick = onClick; return this; }
    public ButtonComponent modifier(Modifier m) { this.modifier = m; return this; }
    public void setHovered(boolean hovered) { this.hovered = hovered; }

    @Override public Modifier modifier() { return modifier; }
    @Override public List<UIComponent> children() { return Collections.emptyList(); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        float textW = font.width(label);
        float textH = font.lineHeight;
        return MeasuredSize.of(
                constraints.constrainWidth(textW + PADDING_H * 2),
                constraints.constrainHeight(textH + PADDING_V * 2)
        );
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
        int bg = hovered ? BG_HOVER_COLOR : BG_COLOR;
        int ix = (int) x, iy = (int) y, iw = (int) width, ih = (int) height;

        // 背景 — 用原版 fill，精确定位
        extractor.fill(ix, iy, ix + iw, iy + ih, bg);

        // 文字 — text() 手动居中
        var font = Minecraft.getInstance().font;
        String txt = label;
        float textW = font.width(txt);
        int textX = (int) (x + (width - textW) / 2f);
        int textY = (int) (y + (height - font.lineHeight) / 2f);

        extractor.text(font, txt, textX, textY, TEXT_COLOR);
    }

    /** 按钮包围盒，用于命中测试。 */
    public LayoutRect hitRect() {
        return LayoutRect.of(x, y, width, height);
    }

    /** 触发点击回调。 */
    public void click() {
        if (onClick != null) onClick.run();
    }
}

