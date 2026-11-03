package dev.anvilcraft.lib.v2.ui.flexible;

import java.util.Locale;

/**
 * 控件贴图的立体风格。
 *
 * <p>风格只决定切图名的前缀，{@link #THREE_D} 的切图不带前缀，因此其路径为空串。
 */
public enum SpriteStyle {
    THREE_D(""),
    FLAT;

    private final String path;

    SpriteStyle() {
        this.path = this.name().toLowerCase(Locale.ROOT);
    }

    SpriteStyle(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}
