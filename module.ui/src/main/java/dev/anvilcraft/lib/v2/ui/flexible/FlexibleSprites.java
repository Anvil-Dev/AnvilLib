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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 柔性控件贴图的查找表。
 *
 * <p>本体贴图由主题、样式、类型、变体四维组合唯一确定，类初始化时一次性登记切图名与四向内边距，
 * 内边距直接取自贴图元数据里的九宫格边框，因此同时给出了本体自带边框与阴影带占据的宽度。
 *
 * <p>指针贴图按固定规则拼名：主题前缀、方向、样式前缀、类型与变体后缀依次拼接，
 * 四个方向各自使用独立原件，朝右指针不需要水平镜像。
 *
 * <p>指针贴图不是九宫格，切图元数据给不出尺寸，宽度与高度只能读原始像素；
 * 该尺寸同时决定指针沿边缘方向占用的长度，是计算指针可移动范围的必要输入。
 */
public final class FlexibleSprites {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Int2ObjectMap<SpriteInfo> SPRITES = new Int2ObjectOpenHashMap<>();
    private static final Map<Identifier, Vector2i> POINTER_SIZES = new HashMap<>();

    static {
        int[] nonShaded = {2, 2, 2, 2};
        int[] shaded1px = {2, 2, 2, 3};
        int[] shaded2px = {2, 2, 2, 4};
        int[] pressed1px = {2, 3, 2, 2};
        int[] pressed2px = {2, 3, 2, 3};
        int[] pressing2px = {2, 3, 2, 2};

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
            int[] padding = switch (type) {
                case NON_SHADED -> nonShaded;
                case SHADED_1PX -> switch (variant) {
                    case HIGHLIGHTED_PRESSED, HIGHLIGHTED_PRESSING, PRESSED -> pressed1px;
                    default -> shaded1px;
                };
                case SHADED_2PX -> switch (variant) {
                    case HIGHLIGHTED_PRESSED, PRESSED -> pressed2px;
                    case HIGHLIGHTED_PRESSING -> pressing2px;
                    default -> shaded2px;
                };
            };
            FlexibleSprites.SPRITES.put(
                FlexibleSprites.toSpriteFlags(theme, style, type, variant),
                new SpriteInfo(
                    AnvilLibUi.location("flex_button/" + path),
                    padding[0],
                    padding[1],
                    padding[2],
                    padding[3],
                    // 顶部边框超过常态 2px 时多出的是阴影带，朝上的指针需要对应下移。
                    Math.max(0, padding[1] - 2)
                )
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
        String themePrefix = theme.getPath().isEmpty() ? "" : theme.getPath() + "_";
        String stylePrefix = style.getPath().isEmpty() ? "" : style.getPath() + "_";
        String variantSuffix = variant.getPath().isEmpty() ? "" : "_" + variant.getPath();
        Identifier texture = AnvilLibUi.location(
            "textures/gui/pointer/" + themePrefix + "pointer_" + direction.getPath() + "_"
                + stylePrefix + type.getPath() + variantSuffix + ".png"
        );
        Vector2i size = FlexibleSprites.pointerSize(texture);
        if (size == null) {
            return null;
        }
        SpriteInfo body = FlexibleSprites.SPRITES.get(FlexibleSprites.toSpriteFlags(theme, style, type, variant));
        return new PointerSprite(texture, size.x, size.y, body.pointerUpInset());
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
            // 指针贴图四周各有 1px 透明边，按可见内容记录尺寸，绘制时再补回这 1px。
            Vector2i size = new Vector2i(Math.max(1, image.getWidth() - 2), Math.max(1, image.getHeight() - 2));
            FlexibleSprites.POINTER_SIZES.put(texture, size);
            return size;
        } catch (IOException exception) {
            FlexibleSprites.LOGGER.warn("Unable to read pointer texture {}", texture, exception);
            return null;
        }
    }

    private static int toSpriteFlags(SpriteTheme theme, SpriteStyle style, SpriteType type, SpriteVariant variant) {
        return theme.ordinal() << 6 | style.ordinal() << 5 | type.ordinal() << 3 | variant.ordinal();
    }

    /**
     * 本体贴图的解析结果，四个方向的边距即控件自身边框带（阴影会加厚对应方向）。
     *
     * <p>边距以不可变的 {@code int} 保存，避免调用方透过共享的向量对象改坏全局查找表。
     *
     * @param pointerUpInset 朝上的指针在本体顶部阴影带之外需要额外下移的像素数
     */
    public record SpriteInfo(
        Identifier sprite,
        int leftPadding,
        int topPadding,
        int rightPadding,
        int bottomPadding,
        int pointerUpInset
    ) {
    }

    /**
     * 已解析出尺寸的指针贴图。
     *
     * @param pointerUpInset 朝上的指针在本体顶部阴影带之外需要额外下移的像素数
     */
    public record PointerSprite(Identifier texture, int width, int height, int pointerUpInset) {
    }
}
