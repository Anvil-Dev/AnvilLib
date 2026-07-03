package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.modifier.ModifierElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 子组件遍历辅助。统一处理 modifier 的约束收缩、尺寸扩展、布局偏移和渲染状态发射。
 * 所有容器组件应通过此类访问子组件的 measure/layout/render,而非直接调用。
 */
public final class LayoutHelper {
    private LayoutHelper() {}

    /**
     * 测量子组件:modifier 收缩约束 → 子组件 measure → modifier 扩展尺寸。
     */
    public static MeasuredSize measureChild(UIComponent child, Constraints parentConstraints) {
        Constraints modC = child.modifier().foldIn(parentConstraints, (c, el) -> el.modifyConstraints(c));
        MeasuredSize size = child.measure(modC);
        return child.modifier().foldOut(size, (el, s) -> el.modifyMeasuredSize(child, modC, s));
    }

    /**
     * 布局子组件:modifier 偏移布局矩形(如 padding 收缩内容区域)。
     */
    public static void layoutChild(UIComponent child, float x, float y, float w, float h) {
        LayoutRect rect = LayoutRect.of(x, y, w, h);
        rect = child.modifier().foldOut(rect, ModifierElement::modifyLayout);
        child.layout(rect.x(), rect.y(), rect.width(), rect.height());
    }

    /**
     * 渲染子组件:从外到内逐层 emit modifier 渲染状态,再调用组件自身的 extractRenderState。
     */
    public static void renderChild(UIComponent child, GuiGraphicsExtractor ext,
                                   float x, float y, float w, float h) {
        LayoutRect[] current = {LayoutRect.of(x, y, w, h)};
        child.modifier().foldIn(null, (ignored, el) -> {
            el.emitRenderState(ext, current[0]);
            current[0] = el.modifyLayout(current[0]);
            return null;
        });
        child.extractRenderState(ext);
    }
}
