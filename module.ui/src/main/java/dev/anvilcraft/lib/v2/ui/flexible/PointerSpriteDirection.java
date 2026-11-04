package dev.anvilcraft.lib.v2.ui.flexible;

import java.util.Locale;

/**
 * 指针贴图实际存在的朝向。
 *
 * <p>朝右没有独立贴图，绘制时把朝左的贴图水平镜像，故此处不含 {@code RIGHT}。
 */
public enum PointerSpriteDirection {
    LEFT,
    UP,
    DOWN;

    private final String path;

    PointerSpriteDirection() {
        this.path = this.name().toLowerCase(Locale.ROOT);
    }

    public String getPath() {
        return this.path;
    }

    public static PointerSpriteDirection of(PointerDirection direction) {
        return switch (direction) {
            case LEFT, RIGHT -> PointerSpriteDirection.LEFT;
            case UP -> PointerSpriteDirection.UP;
            case DOWN -> PointerSpriteDirection.DOWN;
        };
    }
}
