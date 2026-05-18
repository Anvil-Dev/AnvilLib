package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * {@link dev.anvilcraft.lib.v2.ui.Modifier} 链中的单个节点。
 * 每个元素可拦截 measure、layout、render 阶段。
 */
public interface ModifierElement {
    static ModifierElement size(float width, float height) {
        return new SizeElement(width, width, height, height);
    }

    static ModifierElement width(float w) {
        return new SizeElement(0, Float.MAX_VALUE, w, w);
    }

    static ModifierElement height(float h) {
        return new SizeElement(h, 0, h, Float.MAX_VALUE);
    }

    static ModifierElement fillMaxWidth() {
        return new SizeElement(0, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    // ── 工厂方法 ──

    static ModifierElement fillMaxHeight() {
        return new SizeElement(Float.MAX_VALUE, Float.MAX_VALUE, 0, Float.MAX_VALUE);
    }

    static ModifierElement fillMaxSize() {
        return new SizeElement(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    static ModifierElement padding(float all) {
        return new PaddingElement(all, all, all, all);
    }

    static ModifierElement padding(float horizontal, float vertical) {
        return new PaddingElement(horizontal, vertical, horizontal, vertical);
    }

    static ModifierElement background(int color) {
        return new BackgroundElement(color, 0);
    }

    static ModifierElement background(int color, float round) {
        return new BackgroundElement(color, round);
    }

    static ModifierElement border(float width, int color) {
        return new BorderElement(width, color, 0);
    }

    static ModifierElement border(float width, int color, float round) {
        return new BorderElement(width, color, round);
    }

    default Constraints modifyConstraints(Constraints constraints) {
        return constraints;
    }

    default MeasuredSize modifyMeasuredSize(UIComponent component, Constraints constraints, MeasuredSize childSize) {
        return childSize;
    }

    default LayoutRect modifyLayout(LayoutRect rect) {
        return rect;
    }

    default void emitRenderState(GuiGraphicsExtractor extractor, LayoutRect bounds) {
    }
}
