package dev.anvilcraft.lib.v2.ui.flexible;

/**
 * 柔性控件的按钮行为，决定鼠标交互会映射到哪些 {@link SpriteVariant}。
 *
 * <p>{@link #DISPLAY} 始终使用 {@link SpriteVariant#NORMAL}；{@link #CLICK} 只区分常态与悬停，
 * 按住鼠标不会改变外观；{@link #MOMENTARY} 在悬停按住时显示
 * {@link SpriteVariant#HIGHLIGHTED_PRESSING}，松开后回到悬停态；{@link #TOGGLE} 额外读取外部选中状态，
 * 选中但未显示悬停时显示 {@link SpriteVariant#PRESSED}，选中且悬停且没有其它按键抑制时显示
 * {@link SpriteVariant#HIGHLIGHTED_PRESSED}。
 *
 * <p>按压起点落在同一个 Renderer 渲染的按钮上时，未承载按压起点的控件会锁定悬停高亮，
 * 避免拖动到其他按钮时高亮消失。按压起点不落在同一个 Renderer 的按钮上时，控件按未按下处理。
 */
public enum ButtonType {
    CLICK,
    MOMENTARY,
    TOGGLE,
    DISPLAY;

    /**
     * 按输入状态解析当前按钮行为对应的变体。
     */
    public SpriteVariant variant(ButtonInput input) {
        return switch (this) {
            case DISPLAY -> SpriteVariant.NORMAL;
            case CLICK -> input.hovering() ? SpriteVariant.HIGHLIGHTED : SpriteVariant.NORMAL;
            case MOMENTARY -> {
                if (input.pressStartedOnRenderer()
                    && input.pressStartedInside()
                    && input.hovering()
                    && input.allowedPressed()) {
                    yield SpriteVariant.HIGHLIGHTED_PRESSING;
                }
                yield input.hovering() ? SpriteVariant.HIGHLIGHTED : SpriteVariant.NORMAL;
            }
            case TOGGLE -> {
                if (input.pressStartedOnRenderer()
                    && input.pressStartedInside()
                    && input.hovering()
                    && input.allowedPressed()) {
                    yield SpriteVariant.HIGHLIGHTED_PRESSING;
                }
                if (!input.hovering()) {
                    yield input.selected() ? SpriteVariant.PRESSED : SpriteVariant.NORMAL;
                }
                yield input.selected() ? SpriteVariant.HIGHLIGHTED_PRESSED : SpriteVariant.HIGHLIGHTED;
            }
        };
    }
}
