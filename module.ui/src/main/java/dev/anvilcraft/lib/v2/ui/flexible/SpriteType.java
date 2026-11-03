package dev.anvilcraft.lib.v2.ui.flexible;

import java.util.Locale;

/**
 * 控件贴图的阴影类型，决定本体四边的内边距。
 *
 * <p>{@link #NON_SHADED} 四边等宽；两种带阴影类型在底边加厚，按压态改为顶边加厚以表现下沉。
 */
public enum SpriteType {
    NON_SHADED,
    SHADED_1PX,
    SHADED_2PX;

    private final String path;

    SpriteType() {
        this.path = this.name().toLowerCase(Locale.ROOT);
    }

    public String getPath() {
        return this.path;
    }
}
