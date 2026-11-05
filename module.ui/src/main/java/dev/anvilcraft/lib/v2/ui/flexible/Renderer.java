package dev.anvilcraft.lib.v2.ui.flexible;

import dev.anvilcraft.lib.v2.ui.flexible.Renderer.Scope;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * 柔性控件的渲染器基类。
 *
 * <p>渲染器只描述外观（主题、样式、类型、变体）与交互状态，位置、尺寸与文本等每次绘制都会变化的数据
 * 由 {@link Scope} 携带。一个渲染器可以反复 {@link #open()} 出多个作用域，作用域用完后必须关闭，
 * 以便清空其中对 {@link GuiGraphicsExtractor} 等对象的引用。
 */
public abstract class Renderer<T extends Scope> {
    protected SpriteTheme theme = SpriteTheme.LIGHT;
    protected SpriteStyle style = SpriteStyle.THREE_D;
    protected SpriteType type = SpriteType.NON_SHADED;
    protected Function<T, SpriteVariant> variant = ignore -> SpriteVariant.NORMAL;
    protected float alpha = 1.0F;
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

    /**
     * 设置整体透明度，作用于本渲染器后续绘制的本体、指针与文本。
     *
     * <p>透明度只在绘制时读取，因此同一个渲染器可以逐帧改变透明度实现淡入淡出，
     * 不必为每一帧新建渲染器。
     *
     * <p>NaN 会被忽略，透明度保持上一次的取值，避免淡入淡出算出 NaN 时控件整块消失。
     *
     * <p>原版文本着色器在 alpha 低于 0.1 时整体丢弃，淡出末段文本会比本体更早消失。
     *
     * @param alpha 0 到 1 之间的透明度，超出范围时按最近的边界取值
     */
    public Renderer<T> alpha(float alpha) {
        if (Float.isNaN(alpha)) {
            return this;
        }
        this.alpha = Mth.clamp(alpha, 0.0F, 1.0F);
        return this;
    }

    /**
     * 当前透明度，取值 0 到 1。
     */
    public float getAlpha() {
        return this.alpha;
    }

    /**
     * 鼠标按键编号沿用 GLFW 的取值，与公开方法 {@link #basedOnMouse(IntSet)} 的参数约定保持一致。
     *
     * <p>module.ui 的编译期类路径不包含 LWJGL，这里直接写出取值而不引用 {@code GLFW} 常量。
     */
    private static final int MOUSE_BUTTON_LEFT = 0;
    private static final int MOUSE_BUTTON_RIGHT = 1;
    private static final int MOUSE_BUTTON_MIDDLE = 2;

    public Renderer<T> basedOnMouse() {
        return this.basedOnMouse(IntSet.of(Renderer.MOUSE_BUTTON_LEFT, Renderer.MOUSE_BUTTON_RIGHT));
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
                allowedKeys.contains(Renderer.MOUSE_BUTTON_LEFT) && Minecraft.getInstance().mouseHandler.isLeftPressed()
                || allowedKeys.contains(Renderer.MOUSE_BUTTON_MIDDLE) && Minecraft.getInstance().mouseHandler.isMiddlePressed()
                || allowedKeys.contains(Renderer.MOUSE_BUTTON_RIGHT) && Minecraft.getInstance().mouseHandler.isRightPressed()
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
        this.startPressMousePos = new Vector2f(pos);
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
     * <p>作用域持有 {@link GuiGraphicsExtractor} 等游戏对象，关闭后这些引用会被清空，因此每次绘制都应新开一个作用域，
     * 绘制完成后立即关闭。
     */
    public abstract class Scope implements AutoCloseable {
        private @Nullable GuiGraphicsExtractor graphics;
        private @Nullable Vector2i pos;
        protected @Nullable Vector2f mousePos;
        private @Nullable Function<T, Vector2i> size;
        private @Nullable Function<T, Vector2i> hitSize;
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

        public Scope graphics(GuiGraphicsExtractor graphics) {
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
            this.pos = new Vector2i(pos);
            return this;
        }

        public Scope mousePos(int x, int y) {
            return this.mousePos(new Vector2f(x, y));
        }

        public Scope mousePos(Vector2f pos) {
            this.mousePos = new Vector2f(pos);
            return this;
        }

        public Scope size(int width, int height) {
            return this.size(new Vector2i(width, height));
        }

        public Scope size(Vector2i size) {
            Vector2i fixed = new Vector2i(size);
            this.size = ignore -> fixed;
            this.hitSize = null;
            return this;
        }

        public Scope flexSize(int padding) {
            return this.flexSize(padding, padding, padding, padding);
        }

        public Scope flexSize(int horizontalPadding, int verticalPadding) {
            return this.flexSize(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        }

        public Scope flexSize(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
            this.size = scope -> this.flexSizeOf(
                scope, Renderer.this.variant.apply(this.self()), leftPadding, topPadding, rightPadding, bottomPadding
            );
            // 命中判定不能反过来依赖 variant：basedOnMouse 的 variant 会调用 contains，
            // 而 contains 又要取尺寸，若这里复用带 variant 的尺寸函数就会无限递归。
            this.hitSize = scope -> this.flexSizeOf(
                scope, SpriteVariant.NORMAL, leftPadding, topPadding, rightPadding, bottomPadding
            );
            this.textOffset = new Vector2i(leftPadding, topPadding);
            return this;
        }

        private Vector2i flexSizeOf(
            Scope scope,
            SpriteVariant variant,
            int leftPadding,
            int topPadding,
            int rightPadding,
            int bottomPadding
        ) {
            String missing = scope.font == null ? "font" : "";
            missing += scope.text == null ? missing.isEmpty() ? "text" : ", text" : "";
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Some arguments are missing or invalid: %s".formatted(missing));
            }
            FlexibleSprites.SpriteInfo info = FlexibleSprites.pickSprite(
                Renderer.this.theme,
                Renderer.this.style,
                Renderer.this.type,
                variant
            );
            return new Vector2i(
                info.leftPadding() + 1 + leftPadding + scope.font.width(scope.text) + rightPadding + 1 + info.rightPadding(),
                info.topPadding() + 1 + topPadding + scope.font.lineHeight + bottomPadding + 1 + info.bottomPadding()
            );
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
            this.textOffset = new Vector2i(offset);
            return this;
        }

        /**
         * 判断给定点是否落在当前控件范围内，用于鼠标悬停与按压判定。
         *
         * <p>命中判定按常态变体的内边距解析尺寸，不读取当前变体，避免与 {@link #basedOnMouse(IntSet)}
         * 形成“尺寸取变体、变体又取尺寸”的递归。按压态的内边距比常态更厚，命中区域因此可能比按压态的绘制尺寸小 1 像素，这是按常态边距判定的既定行为。
         *
         * @param point 待判断的点，为 {@code null} 表示尚未记录位置，直接视为不在范围内
         */
        public boolean contains(@Nullable Vector2f point) {
            if (point == null || this.pos == null || this.size == null) {
                return false;
            }
            Function<T, Vector2i> resolver = this.hitSize != null ? this.hitSize : this.size;
            Vector2i resolved = resolver.apply(this.self());
            return point.x >= this.pos.x && point.x < this.pos.x + resolved.x
                && point.y >= this.pos.y && point.y < this.pos.y + resolved.y;
        }

        public Scope render() {
            String missing = this.graphics == null ? "graphics" : "";
            missing += this.pos == null ? missing.isEmpty() ? "pos" : ", pos" : "";
            missing += this.size == null ? missing.isEmpty() ? "size" : ", size" : "";
            String absent = this.validate(missing);
            if (!absent.isEmpty()) {
                missing = missing.isEmpty() ? absent : missing + ", " + absent;
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Some arguments are missing or invalid: %s".formatted(missing));
            }
            Vector2i size = this.size.apply(this.self());
            this.render(this.graphics, this.pos.x, this.pos.y, size.x, size.y);
            return this;
        }

        protected abstract void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height);

        /**
         * 返回额外缺失或无效的参数名，由基类统一拼接分隔符。
         */
        protected abstract String validate(String missing);

        @Override
        public void close() {
            this.graphics = null;
            this.pos = null;
            this.mousePos = null;
            this.size = null;
            this.hitSize = null;
            this.font = null;
            this.text = null;
            this.textOffset = null;
        }
    }
}
