package dev.anvilcraft.lib.v2.ui.flexible;

/**
 * 一次按钮状态解析所需的输入。
 *
 * @param selected           {@link ButtonType#TOGGLE} 读取的外部选中状态
 * @param hovering           鼠标是否落在控件范围内
 * @param allowedPressed     允许的鼠标键是否处于按下状态
 * @param pressStartedInside 按压起点是否落在当前控件范围内；该值由 Renderer 记录的按压起点与当前
 *                           控件范围共同决定，因此天然表达“起点在本 Renderer 的某个按钮上”
 */
public record ButtonInput(
    boolean selected,
    boolean hovering,
    boolean allowedPressed,
    boolean pressStartedInside
) {
}
