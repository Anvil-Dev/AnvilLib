package dev.anvilcraft.lib.v2.ui.flexible;

import dev.anvilcraft.lib.v2.ui.flexible.Renderer.Scope;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

/**
 * 柔性控件的渲染器基类。
 *
 * <p>渲染器只描述外观（主题、样式、类型、变体）与交互状态，位置、尺寸与文本等每次绘制都会变化的数据
 * 由 {@link Scope} 携带。一个渲染器可以反复 {@link #open()} 出多个作用域，作用域用完后必须关闭，
 * 以便清空其中对 {@link GuiGraphics} 等对象的引用。
 */
public abstract class Renderer<T extends Scope> {
    protected SpriteTheme theme = SpriteTheme.LIGHT;
    protected SpriteStyle style = SpriteStyle.THREE_D;
    protected SpriteType type = SpriteType.NON_SHADED;
    protected Function<T, SpriteVariant> variant = ignore -> SpriteVariant.NORMAL;
    protected @Nullable Vector2f startPressMousePos;

    public Renderer<T> dark() {
        this.theme = SpriteTheme.DARK;
        return this;
    }

    public Renderer<T> flat() {
        this.style = SpriteStyle.FLAT;
        return this;
    }

    public Renderer<T> shaded1px() {
        this.type = SpriteType.SHADED_1PX;
        return this;
    }

    public Renderer<T> shaded2px() {
        this.type = SpriteType.SHADED_2PX;
        return this;
    }

    public Renderer<T> highlighted() {
        this.variant = ignore -> SpriteVariant.HIGHLIGHTED;
        return this;
    }

    public Renderer<T> pressed() {
        this.variant = ignore -> SpriteVariant.PRESSED;
        return this;
    }

    public Renderer<T> basedOnMouse() {
        return this.basedOnMouse(IntSet.of(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_MOUSE_BUTTON_RIGHT));
    }

    /**
     * 变体由鼠标位置推导：未悬停为常态，悬停未按下为高亮，悬停且按下为按压。
     *
     * <p>按下状态以按下瞬间的鼠标位置是否仍在作用域内判定，这样鼠标按下后再移出控件时不会误判为按压。
     */
    public Renderer<T> basedOnMouse(IntSet allowedKeys) {
        this.variant = scope -> {
            if (scope.mousePos == null) {
                throw new IllegalStateException("Some arguments are missing or invalid: mousePos");
            }
            if (!scope.contains(scope.mousePos)) {
                return SpriteVariant.NORMAL;
            }
            if (!scope.contains(this.startPressMousePos)) {
                return SpriteVariant.HIGHLIGHTED;
            }
            if (
                allowedKeys.contains(GLFW.GLFW_MOUSE_BUTTON_LEFT) && Minecraft.getInstance().mouseHandler.isLeftPressed()
                || allowedKeys.contains(GLFW.GLFW_MOUSE_BUTTON_MIDDLE) && Minecraft.getInstance().mouseHandler.isMiddlePressed()
                || allowedKeys.contains(GLFW.GLFW_MOUSE_BUTTON_RIGHT) && Minecraft.getInstance().mouseHandler.isRightPressed()
            ) {
                return SpriteVariant.PRESSED;
            }
            return SpriteVariant.HIGHLIGHTED;
        };
        return this;
    }

    public void onPress(int x, int y) {
        this.onPress(new Vector2f(x, y));
    }

    public void onPress(Vector2f pos) {
        this.startPressMousePos = pos;
    }

    public void onRelease() {
        this.startPressMousePos = null;
    }

    public SpriteTheme getTheme() {
        return this.theme;
    }

    public SpriteStyle getStyle() {
        return this.style;
    }

    public SpriteType getType() {
        return this.type;
    }

    /**
     * 取当前作用域此刻应使用的变体，落点与绘制时的取法相同。
     */
    public SpriteVariant variantOf(T scope) {
        return this.variant.apply(scope);
    }

    public abstract T open();

    /**
     * 一次绘制所需的数据与收尾动作。
     *
     * <p>作用域持有 {@link GuiGraphics} 等游戏对象，关闭后这些引用会被清空，因此每次绘制都应新开一个作用域，
     * 绘制完成后立即关闭。
     */
    public abstract class Scope implements AutoCloseable {
        private @Nullable GuiGraphics graphics;
        private @Nullable Vector2i pos;
        protected @Nullable Vector2f mousePos;
        private @Nullable Function<T, Vector2i> size;
        protected @Nullable Font font;
        protected @Nullable Component text;
        protected @Nullable Vector2i textOffset;

        protected abstract T self();

        /**
         * 取按当前配置解析出的尺寸，仅在设置过 {@code size} 或 {@code flexSize} 后可用。
         *
         * @return 已解析的宽高；尚未设置尺寸时返回 {@code null}
         */
        public @Nullable Vector2i resolveSize() {
            return this.size == null ? null : this.size.apply(this.self());
        }

        public Scope graphics(GuiGraphics graphics) {
            this.graphics = graphics;
            return this;
        }

        public Scope font(Font font) {
            this.font = font;
            return this;
        }

        public Scope pos(int x, int y) {
            return this.pos(new Vector2i(x, y));
        }

        public Scope pos(Vector2i pos) {
            this.pos = pos;
            return this;
        }

        public Scope mousePos(int x, int y) {
            return this.mousePos(new Vector2f(x, y));
        }

        public Scope mousePos(Vector2f pos) {
            this.mousePos = pos;
            return this;
        }

        public Scope size(int width, int height) {
            return this.size(new Vector2i(width, height));
        }

        public Scope size(Vector2i size) {
            this.size = ignore -> size;
            return this;
        }

        public Scope flexSize(int padding) {
            return this.flexSize(padding, padding, padding, padding);
        }

        public Scope flexSize(int horizontalPadding, int verticalPadding) {
            return this.flexSize(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        }

        public Scope flexSize(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
            this.size = scope -> {
                String missing = scope.font == null ? "font" : "";
                missing += scope.text == null ? missing.isEmpty() ? "text" : ", text" : "";
                if (!missing.isEmpty()) {
                    throw new IllegalStateException("Some arguments are missing or invalid: %s".formatted(missing));
                }
                FlexibleSprites.SpriteInfo info = FlexibleSprites.pickSprite(
                    Renderer.this.theme,
                    Renderer.this.style,
                    Renderer.this.type,
                    Renderer.this.variant.apply(this.self())
                );
                return new Vector2i(
                    info.leftPadding() + 1 + leftPadding + scope.font.width(scope.text) + rightPadding + 1 + info.rightPadding(),
                    info.topPadding() + 1 + topPadding + scope.font.lineHeight + bottomPadding + 1 + info.bottomPadding()
                );
            };
            this.textOffset = new Vector2i(leftPadding, topPadding);
            return this;
        }

        public Scope literal(String value) {
            return this.text(Component.literal(value));
        }

        public Scope translatable(String key) {
            return this.text(Component.translatable(key));
        }

        public Scope translatable(String key, Object... args) {
            return this.text(Component.translatable(key, args));
        }

        public Scope translatableWithFallback(String key, String fallback) {
            return this.text(Component.translatableWithFallback(key, fallback));
        }

        public Scope translatableWithFallback(String key, String fallback, Object... args) {
            return this.text(Component.translatableWithFallback(key, fallback, args));
        }

        public Scope text(Component text) {
            this.text = text;
            return this;
        }

        public Scope textOffset(int offsetX, int offsetY) {
            return this.textOffset(new Vector2i(offsetX, offsetY));
        }

        public Scope textOffset(Vector2i offset) {
            this.textOffset = offset;
            return this;
        }

        /**
         * 判断给定点是否落在当前控件范围内，用于鼠标悬停与按压判定。
         *
         * @param point 待判断的点，为 {@code null} 表示尚未记录位置，直接视为不在范围内
         */
        public boolean contains(@Nullable Vector2f point) {
            if (point == null || this.pos == null || this.size == null) {
                return false;
            }
            Vector2i resolved = this.size.apply(this.self());
            return point.x >= this.pos.x && point.x < this.pos.x + resolved.x
                && point.y >= this.pos.y && point.y < this.pos.y + resolved.y;
        }

        public Scope render() {
            String missing = this.graphics == null ? "graphics" : "";
            missing += this.pos == null ? missing.isEmpty() ? "pos" : ", pos" : "";
            missing += this.size == null ? missing.isEmpty() ? "size" : ", size" : "";
            missing += this.validate(missing);
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Some arguments are missing or invalid: %s".formatted(missing));
            }
            Vector2i size = this.size.apply(this.self());
            this.render(this.graphics, this.pos.x, this.pos.y, size.x, size.y);
            return this;
        }

        protected abstract void render(GuiGraphics graphics, int x, int y, int width, int height);

        protected abstract String validate(String missing);

        @Override
        public void close() {
            this.graphics = null;
            this.pos = null;
            this.mousePos = null;
            this.size = null;
        }
    }
}
