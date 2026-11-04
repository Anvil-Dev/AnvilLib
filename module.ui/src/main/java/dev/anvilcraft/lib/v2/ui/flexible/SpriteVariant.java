package dev.anvilcraft.lib.v2.ui.flexible;

import java.util.Locale;

/**
 * 控件贴图的交互状态。
 *
 * <p>变体只决定切图名的后缀，{@link #NORMAL} 的切图不带后缀，因此其路径为空串。
 */
public enum SpriteVariant {
    NORMAL(""),
    HIGHLIGHTED,
    PRESSED;

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
