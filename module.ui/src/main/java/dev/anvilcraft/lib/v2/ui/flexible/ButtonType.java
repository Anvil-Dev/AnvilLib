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
    TOGGLE;

    /**
     * 按输入状态解析当前按钮行为对应的变体。
     *
     * <p>只要任意鼠标键处于按下状态，未按在该控件上的悬停高亮就会被抑制；
     * {@link #TOGGLE} 在抑制时回落到选中状态，未选中则回到常态。
     */
    public SpriteVariant variant(ButtonInput input) {
        return switch (this) {
            case CLICK -> input.hovering() ? SpriteVariant.HIGHLIGHTED : SpriteVariant.NORMAL;
            case MOMENTARY -> {
                if (!input.anyPressed()) {
                    yield input.hovering() ? SpriteVariant.HIGHLIGHTED : SpriteVariant.NORMAL;
                }
                yield input.hovering() && input.allowedPressed() && input.pressStartedInside()
                    ? SpriteVariant.HIGHLIGHTED_PRESSING
                    : SpriteVariant.NORMAL;
            }
            case TOGGLE -> {
                if (!input.anyPressed()) {
                    if (!input.hovering()) {
                        yield input.selected() ? SpriteVariant.PRESSED : SpriteVariant.NORMAL;
                    }
                    yield input.selected() ? SpriteVariant.HIGHLIGHTED_PRESSED : SpriteVariant.HIGHLIGHTED;
                }
                if (input.hovering() && input.allowedPressed() && input.pressStartedInside()) {
                    yield SpriteVariant.HIGHLIGHTED_PRESSING;
                }
                yield input.selected() ? SpriteVariant.PRESSED : SpriteVariant.NORMAL;
            }
        };
    }
}
