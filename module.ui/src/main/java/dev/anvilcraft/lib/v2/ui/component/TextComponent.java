package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

/**
 * 单行文字渲染。默认样式与原版一致：白色带阴影、左对齐。
 */
public class TextComponent implements UIComponent {

    /** 文字水平对齐方式 */
    public enum Align { LEFT, CENTER, RIGHT }

    private static final int VANILLA_TEXT_COLOR = 0xFFFFFFFF;

    private Modifier modifier;
    private String text;
    private int color = VANILLA_TEXT_COLOR;
    private boolean dropShadow = true;
    private Align align = Align.LEFT;

    private float x, y, width, height;

    public TextComponent(Modifier modifier, String text) {
        this.modifier = modifier;
        this.text = text;
    }

    public TextComponent text(String text) { this.text = text; return this; }
    public TextComponent color(int color) { this.color = color; return this; }
    public TextComponent shadow(boolean enable) { this.dropShadow = enable; return this; }
    public TextComponent align(Align align) { this.align = align; return this; }
    public TextComponent modifier(Modifier m) { this.modifier = m; return this; }

    @Override public Modifier modifier() { return modifier; }
    @Override public List<UIComponent> children() { return Collections.emptyList(); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        float w = font.width(Component.literal(text));
        float h = font.lineHeight;
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
        var font = Minecraft.getInstance().font;
        Component component = Component.literal(text);
        float textW = font.width(component);

        float renderX = (float) switch (align) {
            case LEFT -> x;
            case CENTER -> x + (width - textW) / 2f;
            case RIGHT -> x + width - textW;
        };

        if (dropShadow) {
            extractor.text(font, text, (int) renderX + 1, (int) y + 1, 0x33000000);
        }
        extractor.text(font, text, (int) renderX, (int) y, color);
    }
}

