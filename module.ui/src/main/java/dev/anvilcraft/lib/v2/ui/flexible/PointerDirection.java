package dev.anvilcraft.lib.v2.ui.flexible;

import java.util.Locale;

/**
 * 指针相对于控件本体的朝向，决定指针贴在本体的哪条边上。
 *
 * <p>四个方向各自有独立贴图，贴图名中的方向段直接取 {@link #getPath()}。
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
