package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Constraints;

public record SizeElement(float minWidthHint, float maxWidthHint, float minHeightHint, float maxHeightHint)
        implements ModifierElement {

    @Override
    public Constraints modifyConstraints(Constraints constraints) {
        float minW = minWidthHint > 0 ? Math.max(constraints.minWidth(), minWidthHint) : constraints.minWidth();
        float maxW = maxWidthHint < Float.MAX_VALUE ? Math.min(constraints.maxWidth(), maxWidthHint) : constraints.maxWidth();
        float minH = minHeightHint > 0 ? Math.max(constraints.minHeight(), minHeightHint) : constraints.minHeight();
        float maxH = maxHeightHint < Float.MAX_VALUE ? Math.min(constraints.maxHeight(), maxHeightHint) : constraints.maxHeight();
        return constraints.copy(minW, maxW, minH, maxH);
    }
}
