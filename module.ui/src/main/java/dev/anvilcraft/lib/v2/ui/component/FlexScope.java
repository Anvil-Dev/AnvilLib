package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.UIComponent;
import dev.anvilcraft.lib.v2.ui.UIScope;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link FlexComponent} 的子组件作用域。
 * 通过 {@link #flexGrow(float)} 设置下一个子组件的弹性权重，默认 1。
 */
public class FlexScope extends UIScope {

    private float pendingWeight = 1f;
    private boolean hasPending = false;
    private final List<Float> childWeights = new ArrayList<>();

    /** 设置下一个子组件的弹性权重。0 = 固定尺寸。需在子组件声明前调用。 */
    public FlexScope flexGrow(float weight) {
        this.pendingWeight = weight;
        this.hasPending = true;
        return this;
    }

    @Override
    public void addChild(UIComponent child) {
        float w = this.hasPending ? this.pendingWeight : 1f;
        this.childWeights.add(w);
        this.hasPending = false;
        this.pendingWeight = 1f;
        super.addChild(child);
    }

    public List<Float> childWeights() { return List.copyOf(this.childWeights); }
}
