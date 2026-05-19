package dev.anvilcraft.lib.v2.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

/**
 * 所有 UI 组件的核心接口。
 * <p>
 * 组件每帧参与三个阶段：
 * <ol>
 *   <li>{@link #measure(Constraints)} — 根据父容器约束确定期望尺寸</li>
 *   <li>{@link #layout(float, float, float, float)} — 接收父容器分配的最终位置</li>
 *   <li>{@link #extractRenderState(GuiGraphicsExtractor)} — 提交渲染状态给 GPU</li>
 * </ol>
 * <p>
 * 事件处理方法均有默认空实现，子类只覆写需要的。
 * {@link DeclarativeScreen} 负责递归遍历组件树并分发事件。
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface UIComponent {

    Modifier modifier();

    List<UIComponent> children();

    MeasuredSize measure(Constraints constraints);

    void layout(float x, float y, float width, float height);

    void extractRenderState(GuiGraphicsExtractor extractor);

    // ── 事件处理（默认空实现，子类覆写）──

    /** 鼠标点击。返回 true 表示已消费。 */
    default boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) { return false; }

    /** 鼠标拖拽。返回 true 表示已消费。 */
    default boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) { return false; }

    /** 鼠标释放。 */
    default boolean mouseReleased(MouseButtonEvent event) { return false; }

    /** 滚轮滚动。返回 true 表示已消费。 */
    default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { return false; }

    /** 按键按下。返回 true 表示已消费。 */
    default boolean keyPressed(KeyEvent event) { return false; }

    /** 字符输入。返回 true 表示已消费。 */
    default boolean charTyped(CharacterEvent event) { return false; }
}
