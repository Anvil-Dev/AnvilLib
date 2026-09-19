package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Constraints;

/**
 * 填充父容器可用空间的修饰符元素。
 * 在 modifyConstraints 阶段将 min 提升到父约束的 max,
 * 使子组件被强制撑满对应轴向。
 */
public record FillElement(boolean fillWidth, boolean fillHeight) implements ModifierElement {
    @Override
    public Constraints modifyConstraints(Constraints c) {
        float minW = c.minWidth();
        float maxW = c.maxWidth();
        float minH = c.minHeight();
        float maxH = c.maxHeight();
        if (this.fillWidth() && maxW != Float.MAX_VALUE) {
            minW = maxW;
        }
        if (this.fillHeight() && maxH != Float.MAX_VALUE) {
            minH = maxH;
        }
        return c.copy(minW, maxW, minH, maxH);
    }
}
