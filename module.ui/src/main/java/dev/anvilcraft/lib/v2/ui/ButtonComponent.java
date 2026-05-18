package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

/**
 * A clickable button with background and label.
 */
public class ButtonComponent implements UIComponent {

    private static final int BG_COLOR = 0xFF555555;
    private static final int BG_HOVER_COLOR = 0xFF777777;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final float PADDING_H = 12;
    private static final float PADDING_V = 6;

    private final Modifier modifier;
    private String label;
    private Runnable onClick;

    // layout state
    private float x, y, width, height;

    public ButtonComponent(Modifier modifier, String label, Runnable onClick) {
        this.modifier = modifier;
        this.label = label;
        this.onClick = onClick;
    }

    // ── chained setters ──

    public ButtonComponent label(String label) {
        this.label = label;
        return this;
    }

    public ButtonComponent onClick(Runnable onClick) {
        this.onClick = onClick;
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
        // Background
        SdfGraphics.instance
                .box(x, y, width, height)
                .color(BG_COLOR)
                .round(4)
                .fill()
                .draw(extractor);

        // Label
        var font = Minecraft.getInstance().font;
        Component comp = Component.literal(label);
        float textW = font.width(comp);
        float textH = font.lineHeight;
        float cx = x + (width - textW) / 2;
        float cy = y + (height - textH) / 2;
        extractor.centeredText(font, comp, (int) (x + width / 2), (int) cy, TEXT_COLOR);
    }

    /** Invoke click handler if clicked. Called by hit-testing. */
    void click() {
        if (onClick != null) {
            onClick.run();
        }
    }
}
