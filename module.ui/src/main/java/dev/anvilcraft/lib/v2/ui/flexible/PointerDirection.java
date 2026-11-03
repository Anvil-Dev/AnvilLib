package dev.anvilcraft.lib.v2.ui.flexible;

import java.util.Locale;

/**
 * 指针相对于控件本体的朝向，决定指针贴在本体的哪条边上。
 *
 * <p>贴图只有朝左、朝上、朝下三种原件，朝右由朝左的原件水平镜像得到，
 * 因此 {@link PointerSpriteDirection} 与本文的枚举并不一一对应。
 */
public enum PointerDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN;

    private final String path;

    PointerDirection() {
        this.path = this.name().toLowerCase(Locale.ROOT);
    }

    public String getPath() {
        return this.path;
    }
}
