package dev.anvilcraft.lib.v2.ui.flexible;

/**
 * 一次按钮状态解析所需的输入。
 *
 * @param selected              {@link ButtonType#TOGGLE} 读取的外部选中状态
 * @param hovering              鼠标是否落在控件范围内
 * @param allowedPressed        允许的鼠标键是否处于按下状态
 * @param pressStartedOnRenderer 按压起点是否落在同一个 Renderer 渲染的按钮上
 * @param pressStartedInside    按压起点是否落在当前控件范围内
 */
public record ButtonInput(
    boolean selected,
    boolean hovering,
    boolean allowedPressed,
    boolean pressStartedOnRenderer,
    boolean pressStartedInside
) {
}
