package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Renders a filled rounded rectangle as the component's background.
 */
record BackgroundElement(int color) implements ModifierElement {

    @Override
    public void emitRenderState(GuiGraphicsExtractor extractor, LayoutRect bounds) {
        SdfGraphics.instance
                .box(bounds.x(), bounds.y(), bounds.width(), bounds.height())
                .color(color)
                .fill()
                .draw(extractor);
    }
}
