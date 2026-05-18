package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A single node in a {@link dev.anvilcraft.lib.v2.ui.Modifier} chain.
 * Each element can intercept measure, layout, and render phases.
 */
public interface ModifierElement {

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

    // ── factory methods ──

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
        return new BackgroundElement(color);
    }
}
