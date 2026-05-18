package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record BackgroundElement(int color) implements ModifierElement {

    @Override
    public void emitRenderState(GuiGraphicsExtractor extractor, LayoutRect bounds) {
        SdfGraphics.instance
                .box(bounds.x(), bounds.y(), bounds.width(), bounds.height())
                .color(color)
                .fill()
                .draw(extractor);
    }
}
