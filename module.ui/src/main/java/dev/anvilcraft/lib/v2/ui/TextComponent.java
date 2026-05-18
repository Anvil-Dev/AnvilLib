package dev.anvilcraft.lib.v2.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

/**
 * Renders a single line of Minecraft text.
 */
public class TextComponent implements UIComponent {

    private final Modifier modifier;
    private String text;
    private int color = 0xFFFFFFFF;

    // layout state
    private float x, y, width, height;

    public TextComponent(Modifier modifier, String text) {
        this.modifier = modifier;
        this.text = text;
    }

    // ── chained setters ──

    public TextComponent text(String text) {
        this.text = text;
        return this;
    }

    public TextComponent color(int color) {
        this.color = color;
        return this;
    }

    // ── UIComponent ──

    @Override
    public Modifier modifier() {
        return modifier;
    }

    @Override
    public List<UIComponent> children() {
        return Collections.emptyList();
    }

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
        int textWidth = font.width(component);
        int xPos = (int) (x + (width - textWidth) / 2);
        extractor.centeredText(font, component, (int) (x + width / 2), (int) y, color);
    }
}
