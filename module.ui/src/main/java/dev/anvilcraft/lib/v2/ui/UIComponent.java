package dev.anvilcraft.lib.v2.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

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
 */
public interface UIComponent {

    /** 应用于此组件的修饰符链。 */
    Modifier modifier();

    /** 子组件列表，叶子组件返回空列表。 */
    List<UIComponent> children();

    /**
     * 根据父容器约束测量此组件。容器组件递归测量子组件。
     */
    MeasuredSize measure(Constraints constraints);

    /**
     * 布局阶段后设置最终位置。容器组件在此方法内定位子组件。
     */
    void layout(float x, float y, float width, float height);

    /**
     * 提交渲染状态到 Minecraft GUI 渲染管线。
     * 在 measure+layout 之后调用，每帧一次。
     */
    void extractRenderState(GuiGraphicsExtractor extractor);
}
