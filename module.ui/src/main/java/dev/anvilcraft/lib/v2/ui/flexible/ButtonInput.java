package dev.anvilcraft.lib.v2.ui.flexible;

/**
 * 一次按钮状态解析所需的输入。
 *
 * <p>悬停高亮不再受其它鼠标键影响：{@link ButtonInput#hovering()} 为 {@code true} 时控件一定显示悬停态，
 * 只有按压起点落在本控件范围内且允许的按键仍被按住时才会进入按压态。
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
     * <p>旧实现的 {@code anyPressed}（任意鼠标键被按住）曾用来抑制悬停高亮，该机制已删除，
     * 这里接收后直接忽略，只用于让按旧签名编译的调用方继续链接。新代码请使用四参数规范构造器。
     *
     * <p>注意：依赖旧版“按住任意鼠标键时抑制悬停高亮”行为的调用方，升级后悬停会持续保持高亮。
     *
     * @param anyPressed 旧版“任意鼠标键被按住”标记，已不再参与判定
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
