package dev.anvilcraft.lib.v2.ui.flexible;

/**
 * 柔性控件的按钮行为，决定鼠标交互会映射到哪些 {@link SpriteVariant}。
 *
 * <p>{@link #DISPLAY} 始终使用 {@link SpriteVariant#NORMAL}；{@link #CLICK} 只区分常态与悬停，
 * 按住鼠标不会改变外观；{@link #MOMENTARY} 在悬停按住时显示
 * {@link SpriteVariant#HIGHLIGHTED_PRESSING}，松开后回到悬停态；{@link #TOGGLE} 额外读取外部选中状态，
 * 选中但未悬停时显示 {@link SpriteVariant#PRESSED}，选中且悬停时显示
 * {@link SpriteVariant#HIGHLIGHTED_PRESSED}。
 *
 * <p>悬停高亮不受鼠标按键影响：无论鼠标是在本控件上按下、在同一个 Renderer 的其它按钮上按下，
 * 还是根本没有按下，悬停都保持高亮，拖动到其它按钮时不会闪烁。只有按压起点落在本控件范围内时
 * 才可能进入按压态。
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
                if (input.pressStartedInside() && input.hovering() && input.allowedPressed()) {
                    yield SpriteVariant.HIGHLIGHTED_PRESSING;
                }
                yield input.hovering() ? SpriteVariant.HIGHLIGHTED : SpriteVariant.NORMAL;
            }
            case TOGGLE -> {
                if (input.pressStartedInside() && input.hovering() && input.allowedPressed()) {
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
