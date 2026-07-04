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
        // SdfGraphics 是单例,先 reset 清掉上一次 draw 残留的 stroke/onion 等状态;
        // 非居中模式的 box 以 (x,y) 为中心,故用居中模式 + 传入矩形中心
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
            .fill()
            .draw(extractor);
    }
}
