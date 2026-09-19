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
        // SdfGraphics 是单例,先 reset 清掉上一次 draw 残留状态;
        // box 的 (x,y) 语义为矩形中心,故传入 bounds 中心点
        SdfGraphics.instance
            .reset()
            .box(
                bounds.x() + bounds.width() * 0.5f,
                bounds.y() + bounds.height() * 0.5f,
                bounds.width(), bounds.height()
            )
            .center(true)
            .color(this.color())
            .round(this.round())
            .stroke(this.width())
            .draw(extractor);
    }
}
