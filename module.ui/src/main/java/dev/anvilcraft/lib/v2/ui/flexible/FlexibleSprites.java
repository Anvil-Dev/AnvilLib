package dev.anvilcraft.lib.v2.ui.flexible;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.lib.v2.ui.AnvilLibUi;
import dev.anvilcraft.lib.v2.util.FlattenLoop;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Vector2i;
import org.joml.Vector4i;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 柔性控件贴图的查找表。
 *
 * <p>本体贴图由主题、样式、类型、变体四维组合唯一确定，类初始化时一次性登记切图名与四向内边距，
 * 内边距直接取自贴图元数据里的九宫格边框，因此同时给出了本体自带边框与阴影带占据的宽度。
 *
 * <p>指针贴图的文件名按方向各自成规：朝上的一组只有常态与高亮共用一份不带阴影后缀的原件，
 * 阴影版只出现在按压态；朝左的一组不区分样式；朝下的一组把样式写在前面、且 flat 的常态组省掉了类型后缀。
 * 因此按方向先拼出三条候选名，再由变体直接取对应那条，不需要逐级退化探测。
 *
 * <p>指针贴图不是九宫格，切图元数据给不出尺寸，宽度与高度只能读原始像素；
 * 该尺寸同时决定指针沿边缘方向占用的长度，是计算指针可移动范围的必要输入。
 */
public final class FlexibleSprites {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Int2ObjectMap<SpriteInfo> SPRITES = new Int2ObjectOpenHashMap<>();
    private static final Map<Integer, EnumMap<PointerSpriteDirection, PointerSprites>> POINTERS = new HashMap<>();
    private static final Map<Identifier, Vector2i> POINTER_SIZES = new HashMap<>();

    static {
        Vector4i nonShaded = new Vector4i(2, 2, 2, 2);
        Vector4i shaded1px = new Vector4i(2, 2, 2, 3);
        Vector4i shaded2px = new Vector4i(2, 2, 2, 4);
        Vector4i pressed1px = new Vector4i(2, 3, 2, 2);
        Vector4i pressed2px = new Vector4i(2, 3, 2, 3);

        for (var objs : FlattenLoop.loop(
            SpriteTheme.values(), SpriteStyle.values(), SpriteType.values(), SpriteVariant.values()
        )) {
            SpriteTheme theme = objs.t();
            SpriteStyle style = objs.u();
            SpriteType type = objs.v();
            SpriteVariant variant = objs.w();

            String themePrefix = theme.getPath().isEmpty() ? "" : theme.getPath() + "_";
            String stylePrefix = style.getPath().isEmpty() ? "" : style.getPath() + "_";
            String variantSuffix = variant.getPath().isEmpty() ? "" : "_" + variant.getPath();
            String path = themePrefix + stylePrefix + type.getPath() + variantSuffix;
            Vector4i padding = switch (type) {
                case NON_SHADED -> nonShaded;
                case SHADED_1PX -> variant == SpriteVariant.PRESSED ? pressed1px : shaded1px;
                case SHADED_2PX -> variant == SpriteVariant.PRESSED ? pressed2px : shaded2px;
            };
            FlexibleSprites.SPRITES.put(
                FlexibleSprites.toSpriteFlags(theme, style, type, variant),
                new SpriteInfo(AnvilLibUi.location("flex_button/" + path), padding)
            );
        }
    }

    private FlexibleSprites() {
    }

    public static SpriteInfo pickSprite(
        SpriteTheme theme, SpriteStyle style, SpriteType type, SpriteVariant variant
    ) {
        return FlexibleSprites.SPRITES.get(FlexibleSprites.toSpriteFlags(theme, style, type, variant));
    }

    /**
     * 取与本体同主题、同样式、同类型、同变体且朝向指定方向的指针贴图。
     *
     * @return 指针贴图及其原始像素尺寸；该变体对应方向的贴图缺失时返回 {@code null}
     */
    public static @Nullable PointerSprite pickPointer(
        SpriteTheme theme,
        SpriteStyle style,
        SpriteType type,
        SpriteVariant variant,
        PointerDirection direction
    ) {
        PointerSprites sprites = FlexibleSprites.pointerSprites(theme, style, type)
            .get(PointerSpriteDirection.of(direction));
        Identifier texture = AnvilLibUi.location("textures/gui/pointer/" + sprites.pick(variant) + ".png");
        Vector2i size = FlexibleSprites.pointerSize(texture);
        if (size == null) {
            return null;
        }
        return new PointerSprite(texture, size.x, size.y, direction == PointerDirection.RIGHT);
    }

    private static EnumMap<PointerSpriteDirection, PointerSprites> pointerSprites(
        SpriteTheme theme, SpriteStyle style, SpriteType type
    ) {
        int flags = FlexibleSprites.toPointerFlags(theme, style, type);
        EnumMap<PointerSpriteDirection, PointerSprites> cached = FlexibleSprites.POINTERS.get(flags);
        if (cached != null) {
            return cached;
        }
        String themePrefix = theme.getPath().isEmpty() ? "" : theme.getPath() + "_";
        String stylePrefix = style.getPath().isEmpty() ? "" : style.getPath() + "_";
        EnumMap<PointerSpriteDirection, PointerSprites> pointers = new EnumMap<>(PointerSpriteDirection.class);
        pointers.put(
            PointerSpriteDirection.UP,
            new PointerSprites(
                themePrefix + "pointer_up",
                themePrefix + "pointer_up_highlighted",
                themePrefix + "pointer_up" + (type == SpriteType.NON_SHADED ? "" : "_shaded") + "_pressed"
            )
        );
        pointers.put(
            PointerSpriteDirection.LEFT,
            new PointerSprites(
                themePrefix + "pointer_left" + (type == SpriteType.NON_SHADED ? "" : "_" + type.getPath())
            )
        );
        pointers.put(
            PointerSpriteDirection.DOWN,
            new PointerSprites(
                themePrefix
                    + "pointer_down_"
                    + (
                        style == SpriteStyle.FLAT && type == SpriteType.NON_SHADED
                            ? style.getPath()
                            : stylePrefix + type.getPath()
                    )
            )
        );
        FlexibleSprites.POINTERS.put(flags, pointers);
        return pointers;
    }

    private static @Nullable Vector2i pointerSize(Identifier texture) {
        Vector2i cached = FlexibleSprites.POINTER_SIZES.get(texture);
        if (cached != null) {
            return cached;
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return null;
        }
        try (
            InputStream stream = resource.get().open();
            NativeImage image = NativeImage.read(stream)
        ) {
            Vector2i size = new Vector2i(image.getWidth(), image.getHeight());
            FlexibleSprites.POINTER_SIZES.put(texture, size);
            return size;
        } catch (IOException exception) {
            FlexibleSprites.LOGGER.warn("Unable to read pointer texture {}", texture, exception);
            return null;
        }
    }

    private static int toSpriteFlags(SpriteTheme theme, SpriteStyle style, SpriteType type, SpriteVariant variant) {
        return theme.ordinal() << 5 | style.ordinal() << 4 | type.ordinal() << 2 | variant.ordinal();
    }

    private static int toPointerFlags(SpriteTheme theme, SpriteStyle style, SpriteType type) {
        return theme.ordinal() << 3 | style.ordinal() << 2 | type.ordinal();
    }

    /**
     * 本体贴图的解析结果，四个方向的边距即控件自身边框带（阴影会加厚对应方向）。
     */
    public record SpriteInfo(Identifier sprite, Vector4i padding) {
        public int leftPadding() {
            return this.padding.x;
        }

        public int topPadding() {
            return this.padding.y;
        }

        public int rightPadding() {
            return this.padding.z;
        }

        public int bottomPadding() {
            return this.padding.w;
        }
    }

    /**
     * 已解析出尺寸的指针贴图，{@code mirrored} 表示需要水平镜像后再绘制。
     */
    public record PointerSprite(Identifier texture, int width, int height, boolean mirrored) {
    }

    /**
     * 一个方向下三个变体各自的贴图名。有些方向的高亮与按压贴图只是在常态名上追加后缀，
     * 因此提供只给常态名的构造器，避免每处都手写三条。
     */
    private record PointerSprites(String normal, String highlighted, String pressed) {
        PointerSprites(String normal) {
            this(normal, normal + "_highlighted", normal + "_pressed");
        }

        String pick(SpriteVariant variant) {
            return switch (variant) {
                case NORMAL -> this.normal;
                case HIGHLIGHTED -> this.highlighted;
                case PRESSED -> this.pressed;
            };
        }
    }
}
