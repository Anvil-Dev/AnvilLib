package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 渲染描边圆角矩形边框。
 */
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public record BorderElement(float width, int color, float round) implements ModifierElement {
    public BorderElement(float width, int color) {
        this(width, color, 0);
    }

    @Override
    public void emitRenderState(GuiGraphicsExtractor extractor, LayoutRect bounds) {
        SdfGraphics.instance
            .box(bounds.x(), bounds.y(), bounds.width(), bounds.height())
            .color(color)
            .round(round)
            .stroke(width)
            .draw(extractor);
    }
}
