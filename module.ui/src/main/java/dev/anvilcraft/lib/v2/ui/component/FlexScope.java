package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Composition;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.Ref;
import dev.anvilcraft.lib.v2.ui.UIScope;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link FlexComponent} 的子组件作用域。
 * 通过 {@link #flexGrow(float)} 设置下一个子组件的弹性权重，默认 1。
 */
public class FlexScope extends UIScope {

    private final List<Float> pendingWeights = new ArrayList<>();
    private final List<Float> childWeights = new ArrayList<>();

    /** 设置下一个子组件的弹性权重。0 = 固定尺寸。需在子组件声明前调用。 */
    public FlexScope flexGrow(float weight) {
        this.pendingWeights.add(weight);
        return this;
    }

    private float takeWeight() {
        float w = !this.pendingWeights.isEmpty() ? this.pendingWeights.removeFirst() : 1f;
        this.childWeights.add(w);
        return w;
    }

    @Override
    public TextComponent Text(String text) { this.takeWeight(); return super.Text(text); }

    @Override
    public ButtonComponent Button(String label) { this.takeWeight(); return super.Button(label); }

    @Override
    public CheckboxComponent Checkbox(String label, boolean checked) { this.takeWeight(); return super.Checkbox(label, checked); }

    @Override
    public CheckboxComponent Checkbox(String label, Ref<Boolean> vModel) { this.takeWeight(); return super.Checkbox(label, vModel); }

    @Override
    public SliderComponent Slider(float value, float min, float max, float width) { this.takeWeight(); return super.Slider(value, min, max, width); }

    @Override
    public SliderComponent Slider(float min, float max, float width, Ref<Float> vModel) { this.takeWeight(); return super.Slider(min, max, width, vModel); }

    @Override
    public TextInputComponent TextInput(String placeholder) { this.takeWeight(); return super.TextInput(placeholder); }

    @Override
    public TextInputComponent TextInput(String placeholder, Ref<String> vModel) { this.takeWeight(); return super.TextInput(placeholder, vModel); }

    @Override
    public ImageComponent Image(net.minecraft.resources.Identifier sprite, float width, float height) { this.takeWeight(); return super.Image(sprite, width, height); }

    public List<Float> childWeights() { return List.copyOf(this.childWeights); }
}
