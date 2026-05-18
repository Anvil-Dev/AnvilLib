package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 渲染填充圆角矩形背景。
 */
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public record BackgroundElement(int color, float round) implements ModifierElement {
    public BackgroundElement(int color) {
        this(color, 0);
    }

    @Override
    public void emitRenderState(GuiGraphicsExtractor extractor, LayoutRect bounds) {
        SdfGraphics.instance
            .box(bounds.x(), bounds.y(), bounds.width(), bounds.height())
            .color(color)
            .round(round)
            .fill()
            .draw(extractor);
    }
}
