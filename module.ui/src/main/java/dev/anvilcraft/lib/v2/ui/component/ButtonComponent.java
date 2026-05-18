package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * 可点击按钮。默认样式与原版一致：深灰背景、白色带阴影文字、微圆角。
 */
public class ButtonComponent implements UIComponent {

    // 原版按钮配色
    private static final int BG_COLOR       = 0xFF404040;
    private static final int BG_HOVER_COLOR = 0xFF606060;
    private static final int TEXT_COLOR     = 0xFFFFFFFF;
    private static final int SHADOW_COLOR   = 0x33000000;
    private static final float PADDING_H    = 12;
    private static final float PADDING_V    = 6;
    private static final float ROUND_RADIUS = 2;

    private final Modifier modifier;
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
    void setHovered(boolean hovered) { this.hovered = hovered; }

    @Override public Modifier modifier() { return modifier; }
    @Override public List<UIComponent> children() { return Collections.emptyList(); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        Component comp = Component.literal(label);
        float textW = font.width(comp);
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

        // 背景
        SdfGraphics.instance
                .box(x, y, width, height)
                .color(bg)
                .round(ROUND_RADIUS)
                .fill()
                .draw(extractor);

        // 文字（带阴影，原版风格）
        var font = Minecraft.getInstance().font;
        Component comp = Component.literal(label);
        int centerX = (int) (x + width / 2f);
        int textY = (int) (y + (height - font.lineHeight) / 2f);

        extractor.centeredText(font, comp, centerX + 1, textY + 1, SHADOW_COLOR);
        extractor.centeredText(font, comp, centerX, textY, TEXT_COLOR);
    }

    void click() {
        if (onClick != null) onClick.run();
    }
}

