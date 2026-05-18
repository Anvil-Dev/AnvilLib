package dev.anvilcraft.lib.v2.ui;

/**
 * Modifier element that constrains a component's size.
 */
record SizeElement(float minWidthHint, float maxWidthHint, float minHeightHint, float maxHeightHint)
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
