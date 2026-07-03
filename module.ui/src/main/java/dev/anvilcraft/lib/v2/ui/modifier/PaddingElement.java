package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.UIComponent;

@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public record PaddingElement(float left, float top, float right, float bottom) implements ModifierElement {
    @Override
    public Constraints modifyConstraints(Constraints constraints) {
        float padW = this.left() + this.right();
        float padH = this.top() + this.bottom();
        return constraints.copy(
            Math.max(0, constraints.minWidth() - padW),
            Math.max(0, constraints.maxWidth() - padW),
            Math.max(0, constraints.minHeight() - padH),
            Math.max(0, constraints.maxHeight() - padH)
        );
    }

    @Override
    public MeasuredSize modifyMeasuredSize(UIComponent component, Constraints constraints, MeasuredSize childSize) {
        return MeasuredSize.of(
            childSize.width() + this.left() + this.right(),
            childSize.height() + this.top() + this.bottom()
        );
    }

    @Override
    public LayoutRect modifyLayout(LayoutRect rect) {
        return LayoutRect.of(
            rect.x() + this.left(),
            rect.y() + this.top(),
            rect.width() - this.left() - this.right(),
            rect.height() - this.top() - this.bottom()
        );
    }
}
