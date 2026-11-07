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
    /**
     * 保留旧版五参数构造器的二进制兼容入口。
     *
     * <p>旧实现的 {@code anyPressed}（任意鼠标键被按住）已不再参与判定，这里接收后直接忽略，
     * 只用于让按旧签名编译的调用方继续链接。新代码请使用四参数规范构造器。
     *
     * @param anyPressed 旧版“任意鼠标键被按住”标记，已不再使用
     * @deprecated 改用四参数规范构造器
     */
    @Deprecated
    public ButtonInput(
        boolean selected,
        boolean hovering,
        boolean allowedPressed,
        boolean anyPressed,
        boolean pressStartedInside
    ) {
        this(selected, hovering, allowedPressed, pressStartedInside);
    }
}
