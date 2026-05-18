package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Constraints;

@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public record SizeElement(float minWidthHint, float maxWidthHint, float minHeightHint, float maxHeightHint)
    implements ModifierElement {
    @Override
    public Constraints modifyConstraints(Constraints constraints) {
        float minW = this.minWidthHint() > 0 ? Math.max(constraints.minWidth(), this.minWidthHint()) : constraints.minWidth();
        float maxW = this.maxWidthHint() < Float.MAX_VALUE ? Math.min(constraints.maxWidth(), this.maxWidthHint()) : constraints.maxWidth();
        float minH = this.minHeightHint() > 0 ? Math.max(constraints.minHeight(), this.minHeightHint()) : constraints.minHeight();
        float maxH = this.maxHeightHint() < Float.MAX_VALUE ? Math.min(constraints.maxHeight(), this.maxHeightHint()) : constraints.maxHeight();
        return constraints.copy(minW, maxW, minH, maxH);
    }
}
