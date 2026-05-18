package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.component.BoxComponent;
import dev.anvilcraft.lib.v2.ui.component.ButtonComponent;
import dev.anvilcraft.lib.v2.ui.component.CheckboxComponent;
import dev.anvilcraft.lib.v2.ui.component.ColumnComponent;
import dev.anvilcraft.lib.v2.ui.component.DropdownComponent;
import dev.anvilcraft.lib.v2.ui.component.GridComponent;
import dev.anvilcraft.lib.v2.ui.component.ImageComponent;
import dev.anvilcraft.lib.v2.ui.component.RowComponent;
import dev.anvilcraft.lib.v2.ui.component.ScrollableComponent;
import dev.anvilcraft.lib.v2.ui.component.SliderComponent;
import dev.anvilcraft.lib.v2.ui.component.SpacerComponent;
import dev.anvilcraft.lib.v2.ui.component.TextComponent;
import dev.anvilcraft.lib.v2.ui.component.TextInputComponent;
import dev.anvilcraft.lib.v2.ui.component.scope.BoxScope;
import dev.anvilcraft.lib.v2.ui.component.scope.ColumnScope;
import dev.anvilcraft.lib.v2.ui.component.scope.GridScope;
import dev.anvilcraft.lib.v2.ui.component.scope.RowScope;
import dev.anvilcraft.lib.v2.ui.component.scope.ScrollableScope;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 组件树的构建作用域。
 * <p>
 * 容器组件（Column、Row、Box 等）创建一个 scope，
 * 对其执行内容 lambda，然后收集子组件。
 * <p>
 * 组件构建器在此定义为具体方法，所有 scope 子类自动继承。
 */
public abstract class UIScope {
    final List<UIComponent> children = new ArrayList<>();

    /**
     * 向当前 scope 添加一个子组件。
     */
    public void addChild(UIComponent child) {
        children.add(child);
    }

    /**
     * 返回当前 scope 中已收集的子组件列表（只读）。
     */
    public List<UIComponent> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * 内部方法：recompose 前清空子组件。
     */
    void clearChildren() {
        children.clear();
    }

    // ── 组件构建器 ──

    /**
     * 创建一行文字。
     *
     * @param text 显示文本
     * @return TextComponent 实例，可链式设置颜色、对齐、阴影等
     */
    public TextComponent Text(String text) {
        TextComponent c = new TextComponent(Modifier.NONE, text);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建固定尺寸的空白占位。
     *
     * @param width  宽度（像素）
     * @param height 高度（像素）
     */
    public SpacerComponent Spacer(float width, float height) {
        SpacerComponent c = new SpacerComponent(Modifier.NONE, width, height);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建一个纹理精灵图片。
     *
     * @param sprite 纹理标识符
     * @param width  显示宽度
     * @param height 显示高度
     */
    public ImageComponent Image(Identifier sprite, float width, float height) {
        ImageComponent c = new ImageComponent(Modifier.NONE, sprite, width, height);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建复选框。
     *
     * @param label   标签文字
     * @param checked 初始选中状态
     */
    public CheckboxComponent Checkbox(String label, boolean checked) {
        CheckboxComponent c = new CheckboxComponent(Modifier.NONE, label, checked);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建复选框（vModel 双向绑定）。
     *
     * @param label  标签文字
     * @param vModel {@code Ref<Boolean>}，点击时自动同步值，无需手动 onToggle
     */
    public CheckboxComponent Checkbox(String label, Ref<Boolean> vModel) {
        CheckboxComponent c = new CheckboxComponent(Modifier.NONE, label, vModel.getValue());
        c.onToggle(() -> vModel.setValue(!vModel.getValue()));
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建下拉菜单。
     *
     * @param options        选项列表
     * @param selectedIndex  初始选中索引
     * @param maxPopupHeight 弹出层最大高度
     */
    public DropdownComponent Dropdown(String[] options, int selectedIndex, float maxPopupHeight) {
        DropdownComponent c = new DropdownComponent(Modifier.NONE, options, selectedIndex, maxPopupHeight);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建滑块（手动值）。
     *
     * @param value 初始值
     * @param min   最小值
     * @param max   最大值
     * @param width 轨道宽度（像素）
     */
    public SliderComponent Slider(float value, float min, float max, float width) {
        SliderComponent c = new SliderComponent(Modifier.NONE, value, min, max, width);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建滑块（vModel 双向绑定）。
     *
     * @param min    最小值
     * @param max    最大值
     * @param width  轨道宽度（像素）
     * @param vModel {@code Ref<Float>}，拖拽时自动同步值
     */
    public SliderComponent Slider(float min, float max, float width, Ref<Float> vModel) {
        SliderComponent c = new SliderComponent(Modifier.NONE, vModel.getValue(), min, max, width);
        c.onChange(vModel::setValue);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建单行文本输入框。
     *
     * @param placeholder 占位提示文字（灰色，仅在无输入时显示）
     */
    public TextInputComponent TextInput(String placeholder) {
        TextInputComponent c = new TextInputComponent(Modifier.NONE, placeholder);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建单行文本输入框（vModel 双向绑定）。
     *
     * @param placeholder 占位提示文字
     * @param vModel      {@code Ref<String>}，输入时自动同步值
     */
    public TextInputComponent TextInput(String placeholder, Ref<String> vModel) {
        TextInputComponent c = new TextInputComponent(Modifier.NONE, placeholder);
        c.onChange(vModel::setValue);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建可点击按钮。
     *
     * @param label 按钮文字
     */
    public ButtonComponent Button(String label) {
        ButtonComponent c = new ButtonComponent(Modifier.NONE, label);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建纵向线性布局容器。
     *
     * @param content 子组件声明 lambda
     * @return ColumnComponent 实例，可链式设置 spacing、alignment 等
     */
    public ColumnComponent Column(Consumer<ColumnScope> content) {
        ColumnComponent c = new ColumnComponent(Modifier.NONE);
        ColumnScope inner = new ColumnScope();
        content.accept(inner);
        c.setChildren(inner.getChildren());
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建横向线性布局容器。
     *
     * @param content 子组件声明 lambda
     * @return RowComponent 实例，可链式设置 spacing、alignment 等
     */
    public RowComponent Row(Consumer<RowScope> content) {
        RowComponent c = new RowComponent(Modifier.NONE);
        RowScope inner = new RowScope();
        content.accept(inner);
        c.setChildren(inner.getChildren());
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建层叠布局容器。子组件按声明顺序从底到顶重叠。
     *
     * @param content 子组件声明 lambda
     */
    public BoxComponent Box(Consumer<BoxScope> content) {
        BoxComponent c = new BoxComponent(Modifier.NONE);
        BoxScope inner = new BoxScope();
        content.accept(inner);
        c.setChildren(inner.getChildren());
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建网格布局容器。
     *
     * @param columns 列数
     * @param content 子组件声明 lambda
     * @return GridComponent 实例，可链式设置 spacing
     */
    public GridComponent Grid(int columns, Consumer<GridScope> content) {
        GridComponent c = new GridComponent(Modifier.NONE, columns);
        GridScope inner = new GridScope();
        content.accept(inner);
        c.setChildren(inner.getChildren());
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    /**
     * 创建可滚动容器。内容超过 maxHeight 时可垂直滚动，自动裁剪并显示滚动条。
     *
     * @param maxHeight 容器最大可见高度（像素）
     * @param content   子组件声明 lambda
     */
    public ScrollableComponent Scrollable(float maxHeight, Consumer<ScrollableScope> content) {
        ScrollableComponent c = new ScrollableComponent(Modifier.NONE, maxHeight);
        ScrollableScope inner = new ScrollableScope();
        content.accept(inner);
        c.setChildren(inner.getChildren());
        addChild(c);
        Composition.current().emit(c);
        return c;
    }
}
