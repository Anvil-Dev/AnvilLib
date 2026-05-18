package dev.anvilcraft.lib.v2.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A single node in a {@link Modifier} chain.
 * Each element can intercept measure, layout, and render phases.
 */
public interface ModifierElement {

    /** Modify constraints during the measure pass (e.g. size). */
    default Constraints modifyConstraints(Constraints constraints) {
        return constraints;
    }

    /** Adjust the measured size for padding/offset effects. */
    default MeasuredSize modifyMeasuredSize(UIComponent component, Constraints constraints, MeasuredSize childSize) {
        return childSize;
    }

    /** Transform the layout rect (e.g. apply padding inset). */
    default LayoutRect modifyLayout(LayoutRect rect) {
        return rect;
    }

    /** Emit additional render states (e.g. background, border). */
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
