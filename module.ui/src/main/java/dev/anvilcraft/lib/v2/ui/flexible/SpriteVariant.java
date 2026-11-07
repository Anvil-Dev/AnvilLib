package dev.anvilcraft.lib.v2.ui.flexible;

import java.util.Locale;

/**
 * 控件贴图的交互状态。
 *
 * <p>变体只决定切图名的后缀，{@link #NORMAL} 的切图不带后缀，因此其路径为空串。
 *
 * <p>{@link #PRESSED} 表示选中但未悬停，{@link #HIGHLIGHTED_PRESSED} 表示选中且悬停，
 * {@link #HIGHLIGHTED_PRESSING} 表示悬停且鼠标按钮正被按住。
 *
 * <p>新增变体追加在 {@link #PRESSED} 之后，既有常量的 ordinal 保持不变。
 */
public enum SpriteVariant {
    NORMAL(""),
    HIGHLIGHTED,
    PRESSED,
    HIGHLIGHTED_PRESSED,
    HIGHLIGHTED_PRESSING;

    private final String path;

    SpriteVariant() {
        this.path = this.name().toLowerCase(Locale.ROOT);
    }

    SpriteVariant(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}
