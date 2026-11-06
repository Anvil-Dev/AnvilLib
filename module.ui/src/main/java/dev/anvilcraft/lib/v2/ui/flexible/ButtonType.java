package dev.anvilcraft.lib.v2.ui.flexible;

/**
 * 柔性控件的按钮行为，决定鼠标交互会映射到哪些 {@link SpriteVariant}。
 *
 * <p>{@link #CLICK} 只区分常态与悬停，按住鼠标不会改变外观；{@link #MOMENTARY} 在悬停按住时
 * 显示 {@link SpriteVariant#HIGHLIGHTED_PRESSING}，松开后回到悬停态；{@link #TOGGLE} 额外读取外部选中状态，
 * 选中且未悬停时显示 {@link SpriteVariant#PRESSED}，选中且悬停时显示
 * {@link SpriteVariant#HIGHLIGHTED_PRESSED}。
 */
public enum ButtonType {
    CLICK,
    MOMENTARY,
    TOGGLE
}
