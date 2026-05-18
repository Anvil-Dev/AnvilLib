package dev.anvilcraft.lib.v2.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Core interface for all UI components.
 * <p>
 * Components participate in three phases each frame:
 * <ol>
 *   <li>{@link #measure(Constraints)} — determine desired size given parent constraints</li>
 *   <li>{@link #layout(float, float, float, float)} — receive final position from parent</li>
 *   <li>{@link #extractRenderState(GuiGraphicsExtractor)} — submit render states for GPU rendering</li>
 * </ol>
 */
public interface UIComponent {

    /** The modifier chain applied to this component. */
    Modifier modifier();

    /** Children of this component, or empty list for leaf components. */
    java.util.List<UIComponent> children();

    /**
     * Measure this component given parent constraints.
     * Container components recursively measure children.
     */
    MeasuredSize measure(Constraints constraints);

    /**
     * Set final position after layout pass.
     * Container components position their children.
     */
    void layout(float x, float y, float width, float height);

    /**
     * Submit render states to the Minecraft GUI render pipeline.
     * Called after measure+layout, once per frame.
     */
    void extractRenderState(GuiGraphicsExtractor extractor);
}
