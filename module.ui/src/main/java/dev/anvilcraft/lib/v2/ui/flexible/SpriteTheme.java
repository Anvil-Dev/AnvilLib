package dev.anvilcraft.lib.v2.ui.flexible;

import java.util.Locale;

/**
 * 控件贴图的配色主题。
 *
 * <p>主题只决定切图名的前缀，{@link #LIGHT} 的切图不带前缀，因此其路径为空串。
 */
public enum SpriteTheme {
    LIGHT(""),
    DARK;

    private final String path;

    SpriteTheme() {
        this.path = this.name().toLowerCase(Locale.ROOT);
    }

    SpriteTheme(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}
