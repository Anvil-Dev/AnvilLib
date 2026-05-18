package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.UIComponent;

public record PaddingElement(float left, float top, float right, float bottom) implements ModifierElement {

    @Override
    public MeasuredSize modifyMeasuredSize(UIComponent component, Constraints constraints, MeasuredSize childSize) {
        return MeasuredSize.of(
            childSize.width() + left + right,
            childSize.height() + top + bottom
        );
    }

    @Override
    public LayoutRect modifyLayout(LayoutRect rect) {
        return LayoutRect.of(
            rect.x() + left,
            rect.y() + top,
            rect.width() - left - right,
            rect.height() - top - bottom
        );
    }
}
