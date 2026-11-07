package dev.anvilcraft.lib.v2.test.flexible;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.lib.v2.ui.flexible.ButtonInput;
import dev.anvilcraft.lib.v2.ui.flexible.ButtonRenderer;
import dev.anvilcraft.lib.v2.ui.flexible.ButtonType;
import dev.anvilcraft.lib.v2.ui.flexible.FlexibleSprites;
import dev.anvilcraft.lib.v2.ui.flexible.FlyoutRenderer;
import dev.anvilcraft.lib.v2.ui.flexible.PointerDirection;
import dev.anvilcraft.lib.v2.ui.flexible.SpriteStyle;
import dev.anvilcraft.lib.v2.ui.flexible.SpriteTheme;
import dev.anvilcraft.lib.v2.ui.flexible.SpriteType;
import dev.anvilcraft.lib.v2.ui.flexible.SpriteVariant;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 柔性控件的游戏内回归屏幕。
 *
 * <p>打开时按主题、样式、类型、变体四维组合逐项核对本体贴图：切图必须落在 {@code anvillib_ui} 命名空间下、
 * 能从 {@code minecraft:gui} 图集取到，且九宫格边框必须与 {@link FlexibleSprites} 登记的内边距逐边相等；
 * 随后核对四个方向的指针贴图尺寸、命名空间与绘制矩形，验证 {@code flexSize} 的尺寸算式、{@code contains} 的边界判定，
 * 以及整体透明度对本体与文本颜色的作用。
 *
 * <p>核对完成后屏幕把全部组合绘制成按钮网格，并用四个方向的浮窗展示指针贴图，便于肉眼确认外观。
 */
public final class FlexibleTestScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int FLEX_PADDING = 3;

    private final List<ButtonRenderer> bodies = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private int checks;
    private int renderedFrames;
    private boolean layoutChecked;

    public FlexibleTestScreen() {
        super(Component.literal("AnvilLib flexible controls"));
        for (SpriteTheme theme : SpriteTheme.values()) {
            for (SpriteStyle style : SpriteStyle.values()) {
                for (SpriteType type : SpriteType.values()) {
                    for (SpriteVariant variant : SpriteVariant.values()) {
                        this.bodies.add(FlexibleTestScreen.create(theme, style, type, variant));
                        this.labels.add("%s %s %s %s".formatted(theme, style, type, variant));
                    }
                }
            }
        }
    }

    private static ButtonRenderer create(
        SpriteTheme theme,
        SpriteStyle style,
        SpriteType type,
        SpriteVariant variant
    ) {
        ButtonRenderer renderer = new ButtonRenderer();
        if (theme == SpriteTheme.DARK) {
            renderer.dark();
        }
        if (style == SpriteStyle.FLAT) {
            renderer.flat();
        }
        switch (type) {
            case SHADED_1PX -> renderer.shaded1px();
            case SHADED_2PX -> renderer.shaded2px();
            case NON_SHADED -> {
            }
        }
        switch (variant) {
            case HIGHLIGHTED -> renderer.highlighted();
            case HIGHLIGHTED_PRESSED -> renderer.highlightedPressed();
            case HIGHLIGHTED_PRESSING -> renderer.highlightedPressing();
            case PRESSED -> renderer.pressed();
            case NORMAL -> {
            }
        }
        return renderer;
    }

    @Override
    protected void init() {
        this.failures.clear();
        this.checks = 0;
        this.verifyBodies();
        this.verifyPointers();
        this.verifyPointerQuad();
        this.verifyFlexSize();
        this.verifyTextColor();
        this.verifyAlpha();
        this.verifyShowcaseSizes();
        this.verifyMouseVariantWithFlexSize();
        this.verifyButtonTypes();
        this.verifyContains();
        if (this.failures.isEmpty()) {
            LOGGER.info("FLEXIBLE_TEST ALL PASS: {} checks", this.checks);
        } else {
            LOGGER.error("FLEXIBLE_TEST FAILED: {} of {} checks failed, details: {}", this.failures.size(), this.checks, this.failures);
        }
    }

    private void verifyBodies() {
        GuiSpriteManager sprites = Minecraft.getInstance().getGuiSprites();
        for (SpriteTheme theme : SpriteTheme.values()) {
            for (SpriteStyle style : SpriteStyle.values()) {
                for (SpriteType type : SpriteType.values()) {
                    for (SpriteVariant variant : SpriteVariant.values()) {
                        this.verifyBody(sprites, FlexibleTestScreen.create(theme, style, type, variant));
                    }
                }
            }
        }
    }

    private void verifyBody(GuiSpriteManager sprites, ButtonRenderer renderer) {
        FlexibleSprites.SpriteInfo info;
        try (ButtonRenderer.Scope scope = renderer.open()) {
            info = FlexibleSprites.pickSprite(
                renderer.getTheme(), renderer.getStyle(), renderer.getType(), renderer.variantOf(scope)
            );
        }
        String label = info.sprite().toString();
        this.check(
            info.sprite().getNamespace().equals("anvillib_ui"),
            label + " 必须位于 anvillib_ui 命名空间"
        );
        TextureAtlasSprite sprite = sprites.getSprite(info.sprite());
        this.check(
            sprite.contents().name().equals(info.sprite()),
            label + " 必须能在 minecraft:gui 图集中解析，实际解析到 " + sprite.contents().name()
        );
        GuiSpriteScaling scaling = sprites.getSpriteScaling(sprite);
        if (scaling instanceof GuiSpriteScaling.NineSlice slice) {
            GuiSpriteScaling.NineSlice.Border border = slice.border();
            this.check(
                border.left() == info.leftPadding()
                    && border.top() == info.topPadding()
                    && border.right() == info.rightPadding()
                    && border.bottom() == info.bottomPadding(),
                label + " 的九宫格边框必须与登记内边距一致，贴图为 " + border + "，登记为 " + info.leftPadding() + "/" + info.topPadding() + "/" + info.rightPadding() + "/" + info.bottomPadding()
            );
        } else {
            this.check(false, label + " 必须声明 nine_slice 缩放，实际为 " + scaling);
        }
    }

    private void verifyPointers() {
        for (SpriteTheme theme : SpriteTheme.values()) {
            for (SpriteStyle style : SpriteStyle.values()) {
                for (SpriteType type : SpriteType.values()) {
                    for (SpriteVariant variant : SpriteVariant.values()) {
                        for (PointerDirection direction : PointerDirection.values()) {
                            this.verifyPointer(theme, style, type, variant, direction);
                        }
                    }
                }
            }
        }
    }

    /**
     * 指针绘制矩形必须补回四周 1px 透明边。
     */
    private void verifyPointerQuad() {
        FlyoutRenderer.PointerQuad quad = FlyoutRenderer.pointerQuad(100, 50, 4, 3);
        this.check(
            quad.width() == 6 && quad.height() == 5,
            "指针绘制矩形必须补回四周 1px 透明边，实际为 " + quad.width() + "x" + quad.height()
        );
        this.check(
            quad.x() == 99 && quad.y() == 49,
            "指针绘制起点必须向左上各扩 1px，实际为 " + quad.x() + "," + quad.y()
        );
    }

    private void verifyPointer(
        SpriteTheme theme,
        SpriteStyle style,
        SpriteType type,
        SpriteVariant variant,
        PointerDirection direction
    ) {
        String label = "%s/%s/%s/%s 朝 %s 的指针".formatted(theme, style, type, variant, direction);
        FlexibleSprites.PointerSprite pointer = FlexibleSprites.pickPointer(theme, style, type, variant, direction);
        if (pointer == null) {
            this.check(false, label + "必须存在");
            return;
        }
        this.check(pointer.width() > 0 && pointer.height() > 0, label + "必须具有正的像素尺寸");
        ResourceLocation texture = pointer.texture();
        this.check(
            texture.getNamespace().equals("anvillib_ui"),
            label + "必须位于 anvillib_ui 命名空间，实际为 " + texture
        );
    }

    private void verifyFlexSize() {
        ButtonRenderer renderer = new ButtonRenderer();
        renderer.shaded2px();
        renderer.pressed();
        Component text = Component.literal("flex");
        FlexibleSprites.SpriteInfo info;
        try (ButtonRenderer.Scope scope = renderer.open()) {
            scope.font(this.font);
            scope.text(text);
            info = FlexibleSprites.pickSprite(
                renderer.getTheme(), renderer.getStyle(), renderer.getType(), renderer.variantOf(scope)
            );
        }
        Vector2i size;
        try (ButtonRenderer.Scope sized = renderer.open()) {
            sized.font(this.font);
            sized.text(text);
            sized.flexSize(FLEX_PADDING);
            size = sized.resolveSize();
        }
        this.check(size != null, "flexSize 必须能解析出尺寸");
        if (size == null) {
            return;
        }
        int expectedWidth = info.leftPadding() + 1 + FLEX_PADDING * 2 + this.font.width(text) + 1 + info.rightPadding();
        int expectedHeight = info.topPadding() + 1 + FLEX_PADDING * 2 + this.font.lineHeight + 1 + info.bottomPadding();
        this.check(
            size.x == expectedWidth && size.y == expectedHeight,
            "flexSize 必须按内边距与文本尺寸计算，期望 " + expectedWidth + "x" + expectedHeight + "，实际为 " + size
        );
    }

    /**
     * 文本颜色必须自带不透明 alpha，否则透明颜色会让标签完全不可见。
     */
    private void verifyTextColor() {
        int plain = ButtonRenderer.textColor(Component.literal("text"));
        this.check(
            (plain & 0xFF000000) == 0xFF000000,
            "默认文本颜色必须不透明，实际为 " + Integer.toHexString(plain)
        );
        this.check(
            (plain & 0xFFFFFF) == 0xFFFFFF,
            "默认文本颜色必须是白色，实际为 " + Integer.toHexString(plain)
        );

        int styled = ButtonRenderer.textColor(Component.literal("text").withStyle(ChatFormatting.RED));
        this.check(
            (styled & 0xFF000000) == 0xFF000000,
            "样式文本颜色必须不透明，实际为 " + Integer.toHexString(styled)
        );
        this.check(
            (styled & 0xFFFFFF) == 0xFF5555,
            "样式文本颜色必须保留原始 RGB，实际为 " + Integer.toHexString(styled)
        );
    }

    /**
     * {@code basedOnMouse} 的变体依赖 {@code contains}，而 {@code flexSize} 的尺寸又依赖变体，
     * 两者组合必须仍能解析尺寸与命中，不能递归溢出。
     */
    private void verifyMouseVariantWithFlexSize() {
        ButtonRenderer renderer = new ButtonRenderer();
        renderer.basedOnMouse();
        ButtonRenderer.Scope scope = renderer.open();
        scope.font(this.font);
        scope.text(Component.literal("hover"));
        scope.flexSize(FLEX_PADDING);
        scope.pos(10, 10);
        scope.mousePos(15, 15);
        try {
            Vector2i size = scope.resolveSize();
            this.check(size != null && size.x > 0 && size.y > 0, "basedOnMouse 与 flexSize 组合必须解析出正尺寸");
            this.check(scope.contains(new Vector2f(15, 15)), "basedOnMouse 与 flexSize 组合必须命中控件内部");
            this.check(!scope.contains(new Vector2f(100, 100)), "basedOnMouse 与 flexSize 组合必须排除控件外部");
            this.check(renderer.variantOf(scope) == SpriteVariant.NORMAL, "默认 DISPLAY 必须始终解析为 NORMAL");
            renderer.buttonType(ButtonType.MOMENTARY);
            this.check(renderer.variantOf(scope) == SpriteVariant.HIGHLIGHTED, "MOMENTARY 悬停时必须解析为 HIGHLIGHTED");
            renderer.buttonType(ButtonType.CLICK);
            this.check(renderer.variantOf(scope) == SpriteVariant.HIGHLIGHTED, "CLICK 悬停时必须解析为 HIGHLIGHTED");
            renderer.buttonType(ButtonType.MOMENTARY);
            renderer.onPress(new Vector2f(15, 15));
            this.check(renderer.variantOf(scope) == SpriteVariant.HIGHLIGHTED, "起点在控件内且按键未被按住时必须为 HIGHLIGHTED");
            renderer.onRelease();
            this.check(renderer.variantOf(scope) == SpriteVariant.HIGHLIGHTED, "松开后必须回到 HIGHLIGHTED");
            PressedButtonRenderer pressed = new PressedButtonRenderer();
            pressed.basedOnMouse().buttonType(ButtonType.MOMENTARY);
            ButtonRenderer.Scope pressedScope = pressed.open();
            pressedScope.font(this.font).text(Component.literal("press")).flexSize(FLEX_PADDING)
                .pos(10, 10).mousePos(15, 15);
            try {
                pressed.onPress(new Vector2f(15, 15));
                this.check(
                    pressed.variantOf(pressedScope) == SpriteVariant.HIGHLIGHTED_PRESSING,
                    "起点在控件内且允许键被按住时必须解析为 HIGHLIGHTED_PRESSING"
                );
                pressed.onRelease();
                this.check(
                    pressed.variantOf(pressedScope) == SpriteVariant.HIGHLIGHTED,
                    "松开后必须从 HIGHLIGHTED_PRESSING 回到 HIGHLIGHTED"
                );
            } finally {
                pressedScope.close();
            }
        } catch (StackOverflowError | RuntimeException error) {
            this.check(false, "basedOnMouse 与 flexSize 组合不得抛出异常或递归溢出");
        } finally {
            scope.close();
        }
    }

    /**
     * 透明度必须按 0 到 1 钳制，并按 alpha 分量作用到本体与文本的颜色上。
     *
     * <p>透明度只体现在 ARGB 的 alpha 分量上，文本同时保留自带颜色。
     */
    private void verifyAlpha() {
        ButtonRenderer renderer = new ButtonRenderer();
        this.check(renderer.alpha(0.5F) == renderer, "alpha 必须返回渲染器自身以便链式调用");
        renderer.alpha(-1.0F);
        this.check(renderer.getAlpha() == 0.0F, "alpha 必须把负值钳制到 0，实际为 " + renderer.getAlpha());
        renderer.alpha(2.0F);
        this.check(renderer.getAlpha() == 1.0F, "alpha 必须把大于 1 的取值钳制到 1，实际为 " + renderer.getAlpha());
        renderer.alpha(0.5F);
        this.check(renderer.getAlpha() == 0.5F, "alpha 必须原样保留 0 到 1 之间的取值，实际为 " + renderer.getAlpha());
        renderer.alpha(Float.NaN);
        this.check(renderer.getAlpha() == 0.5F, "NaN 必须被忽略并保持上一次透明度，实际为 " + renderer.getAlpha());
        this.check(
            ButtonRenderer.color(1.0F) == 0xFFFFFFFF,
            "不透明本体颜色必须是白色 0xFFFFFFFF，实际为 " + Integer.toHexString(ButtonRenderer.color(1.0F))
        );
        this.check(
            ButtonRenderer.color(0.0F) == 0x00FFFFFF,
            "全透明本体颜色必须保留白色 RGB 且 alpha 为 0，实际为 " + Integer.toHexString(ButtonRenderer.color(0.0F))
        );
        this.check(
            (ButtonRenderer.color(0.5F) >>> 24) == 127,
            "半透明本体颜色的 alpha 必须为 127，实际为 " + (ButtonRenderer.color(0.5F) >>> 24)
        );
        Component text = Component.literal("alpha");
        this.check(
            ButtonRenderer.textColor(text, 1.0F) == ButtonRenderer.textColor(text),
            "不透明文本颜色必须与未指定透明度时一致"
        );
        int transparentText = ButtonRenderer.textColor(text, 0.0F);
        this.check(
            (transparentText >>> 24) == 0 && (transparentText & 0xFFFFFF) == 0xFFFFFF,
            "全透明文本必须保留白色 RGB 且 alpha 为 0，实际为 " + Integer.toHexString(transparentText)
        );
    }

    /**
     * 展示用例的尺寸约定：自适应随文本增长，固定尺寸原样返回，深色浮窗同样能解析尺寸。
     */
    private void verifyShowcaseSizes() {
        ButtonRenderer shortText = new ButtonRenderer();
        ButtonRenderer.Scope shortScope = shortText.open();
        shortScope.font(this.font);
        shortScope.text(Component.literal("OK"));
        shortScope.flexSize(FLEX_PADDING);
        Vector2i shortSize = shortScope.resolveSize();
        shortScope.close();

        ButtonRenderer longText = new ButtonRenderer();
        ButtonRenderer.Scope longScope = longText.open();
        longScope.font(this.font);
        longScope.text(Component.literal("a considerably longer label"));
        longScope.flexSize(FLEX_PADDING);
        Vector2i longSize = longScope.resolveSize();
        longScope.close();

        this.check(
            shortSize != null && longSize != null && longSize.x > shortSize.x,
            "更长的文本必须得到更宽的自适应控件"
        );

        ButtonRenderer fixed = new ButtonRenderer();
        ButtonRenderer.Scope fixedScope = fixed.open();
        fixedScope.font(this.font);
        fixedScope.size(180, 36);
        Vector2i fixedSize = fixedScope.resolveSize();
        fixedScope.close();
        this.check(
            fixedSize != null && fixedSize.x == 180 && fixedSize.y == 36,
            "固定尺寸必须原样返回，实际为 " + fixedSize
        );

        FlyoutRenderer dark = new FlyoutRenderer();
        dark.dark();
        FlyoutRenderer.Scope darkScope = dark.open();
        darkScope.pointer(PointerDirection.UP, 0.25F);
        darkScope.font(this.font);
        darkScope.text(Component.literal("dark"));
        darkScope.flexSize(FLEX_PADDING);
        Vector2i darkSize = darkScope.resolveSize();
        darkScope.close();
        this.check(darkSize != null && darkSize.x > 0, "深色浮窗必须能解析出正尺寸");
    }

    private void verifyButtonTypes() {
        this.check(
            ButtonType.DISPLAY.variant(new ButtonInput(false, true, true, true)) == SpriteVariant.NORMAL,
            "DISPLAY 在悬停并按住时也必须保持 NORMAL"
        );
        this.check(
            ButtonType.DISPLAY.variant(new ButtonInput(true, false, false, false)) == SpriteVariant.NORMAL,
            "DISPLAY 在选中且未悬停时也必须保持 NORMAL"
        );
        this.check(
            ButtonType.CLICK.variant(new ButtonInput(false, true, false, false)) == SpriteVariant.HIGHLIGHTED,
            "CLICK 悬停时必须为 HIGHLIGHTED"
        );
        this.check(
            ButtonType.CLICK.variant(new ButtonInput(false, false, true, true)) == SpriteVariant.NORMAL,
            "CLICK 未悬停时必须保持 NORMAL"
        );
        this.check(
            ButtonType.MOMENTARY.variant(new ButtonInput(false, false, false, false)) == SpriteVariant.NORMAL,
            "MOMENTARY 未悬停且未按住时必须为 NORMAL"
        );
        this.check(
            ButtonType.MOMENTARY.variant(new ButtonInput(false, true, false, false)) == SpriteVariant.HIGHLIGHTED,
            "MOMENTARY 悬停且未按住时必须为 HIGHLIGHTED"
        );
        this.check(
            ButtonType.MOMENTARY.variant(new ButtonInput(false, true, true, true)) == SpriteVariant.HIGHLIGHTED_PRESSING,
            "MOMENTARY 在控件内按住允许键时必须为 HIGHLIGHTED_PRESSING"
        );
        this.check(
            ButtonType.MOMENTARY.variant(new ButtonInput(false, true, true, false)) == SpriteVariant.HIGHLIGHTED,
            "MOMENTARY 按压起点在控件外时必须为 HIGHLIGHTED"
        );
        this.check(
            ButtonType.MOMENTARY.variant(new ButtonInput(false, true, false, true)) == SpriteVariant.HIGHLIGHTED,
            "MOMENTARY 按键不被允许时必须为 HIGHLIGHTED"
        );
        this.check(
            ButtonType.MOMENTARY.variant(new ButtonInput(false, false, true, true)) == SpriteVariant.NORMAL,
            "MOMENTARY 按压后鼠标移出时必须为 NORMAL"
        );
        this.check(
            ButtonType.TOGGLE.variant(new ButtonInput(true, false, false, false)) == SpriteVariant.PRESSED,
            "TOGGLE 选中且未悬停时必须为 PRESSED"
        );
        this.check(
            ButtonType.TOGGLE.variant(new ButtonInput(false, false, false, false)) == SpriteVariant.NORMAL,
            "TOGGLE 未选中且未悬停时必须为 NORMAL"
        );
        this.check(
            ButtonType.TOGGLE.variant(new ButtonInput(true, true, false, false)) == SpriteVariant.HIGHLIGHTED_PRESSED,
            "TOGGLE 选中且悬停时必须为 HIGHLIGHTED_PRESSED"
        );
        this.check(
            ButtonType.TOGGLE.variant(new ButtonInput(false, true, false, false)) == SpriteVariant.HIGHLIGHTED,
            "TOGGLE 未选中且悬停时必须为 HIGHLIGHTED"
        );
        this.check(
            ButtonType.TOGGLE.variant(new ButtonInput(true, true, false, true)) == SpriteVariant.HIGHLIGHTED_PRESSED,
            "TOGGLE 选中悬停且按键不被允许时必须为 HIGHLIGHTED_PRESSED"
        );
        this.check(
            ButtonType.TOGGLE.variant(new ButtonInput(true, true, true, true)) == SpriteVariant.HIGHLIGHTED_PRESSING,
            "TOGGLE 选中悬停且在控件内按住允许键时必须为 HIGHLIGHTED_PRESSING"
        );
        this.check(
            ButtonType.TOGGLE.variant(new ButtonInput(true, true, true, false)) == SpriteVariant.HIGHLIGHTED_PRESSED,
            "TOGGLE 选中悬停且按压起点在控件外时必须为 HIGHLIGHTED_PRESSED"
        );
        this.check(
            ButtonType.TOGGLE.variant(new ButtonInput(false, true, false, true)) == SpriteVariant.HIGHLIGHTED,
            "TOGGLE 未选中悬停且按键不被允许时必须为 HIGHLIGHTED"
        );
    }

    private void verifyContains() {
        ButtonRenderer renderer = new ButtonRenderer();
        try (ButtonRenderer.Scope scope = renderer.open()) {
            scope.pos(10, 20);
            scope.size(30, 40);
            this.check(scope.contains(new Vector2f(10, 20)), "contains 必须包含左上角");
            this.check(scope.contains(new Vector2f(39, 59)), "contains 必须包含右下角内侧");
            this.check(!scope.contains(new Vector2f(40, 40)), "contains 必须排除右边界之外");
            this.check(!scope.contains(new Vector2f(20, 60)), "contains 必须排除下边界之外");
            this.check(!scope.contains(new Vector2f(9, 30)), "contains 必须排除左边界之外");
            this.check(!scope.contains(null), "contains 必须把空坐标视为不在范围内");
        }
        try (ButtonRenderer.Scope empty = renderer.open()) {
            this.check(!empty.contains(new Vector2f(0, 0)), "未设置位置的控件必须不包含任何点");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF101014);
        graphics.drawString(this.font, this.title, 8, 8, 0xFFFFFFFF, false);
        String status = this.failures.isEmpty()
            ? "PASS " + this.checks + " checks"
            : "FAIL " + this.failures.size() + " of " + this.checks + " checks";
        graphics.drawString(
            this.font,
            status,
            8,
            20,
            this.failures.isEmpty() ? 0xFF55FF55 : 0xFFFF5555,
            false
        );
        int cursorX = 8;
        int cursorY = 40;
        int rowHeight = 0;
        int gap = 10;
        String[] flexTexts = {"OK", "确定", "flex", "自适应文本"};

        for (int mode = 0; mode < 2; mode++) {
            for (int index = 0; index < this.bodies.size(); index++) {
                ButtonRenderer.Scope scope = this.bodies.get(index).open();
                scope.graphics(graphics);
                scope.font(this.font);
                if (mode == 0) {
                    scope.text(Component.literal(flexTexts[index % flexTexts.length]));
                    scope.flexSize(FLEX_PADDING);
                } else {
                    scope.size(64 + index % 3 * 34, 20 + index % 3 * 5);
                    scope.text(Component.literal(FlexibleTestScreen.shortLabel(this.labels.get(index))));
                }
                Vector2i size = scope.resolveSize();
                int width = size == null ? 64 : size.x;
                int height = size == null ? 20 : size.y;
                if (cursorX + width + gap > this.width) {
                    cursorX = 8;
                    cursorY += rowHeight + gap;
                    rowHeight = 0;
                }
                scope.pos(cursorX, cursorY);
                scope.render();
                scope.close();
                cursorX += width + gap;
                rowHeight = Math.max(rowHeight, height);
            }
            cursorX = 8;
            cursorY += rowHeight + gap;
            rowHeight = 0;
        }

        int flyoutMargin = 12;
        for (SpriteTheme theme : SpriteTheme.values()) {
            for (SpriteStyle style : SpriteStyle.values()) {
                for (SpriteType type : SpriteType.values()) {
                    for (SpriteVariant variant : SpriteVariant.values()) {
                        for (PointerDirection direction : PointerDirection.values()) {
                            FlyoutRenderer renderer = FlexibleTestScreen.createFlyout(theme, style, type, variant, direction);
                            FlyoutRenderer.Scope scope = renderer.open();
                            scope.pointer(direction, 0.5F);
                            scope.graphics(graphics);
                            scope.font(this.font);
                            scope.flexSize(FLEX_PADDING);
                            scope.text(Component.literal(direction.name().substring(0, 1)));
                            Vector2i size = scope.resolveSize();
                            int outerWidth = (size == null ? 24 : size.x) + flyoutMargin * 2;
                            int outerHeight = (size == null ? 16 : size.y) + flyoutMargin * 2;
                            if (cursorX + outerWidth + gap > this.width) {
                                cursorX = 8;
                                cursorY += rowHeight + gap;
                                rowHeight = 0;
                            }
                            scope.pos(cursorX + flyoutMargin, cursorY + flyoutMargin);
                            scope.render();
                            scope.close();
                            cursorX += outerWidth + gap;
                            rowHeight = Math.max(rowHeight, outerHeight);
                        }
                    }
                }
            }
        }

        cursorX = 8;
        cursorY += rowHeight + gap * 2;
        rowHeight = 0;
        float[] percents = {0.0F, 0.5F, 1.0F};
        for (SpriteTheme theme : SpriteTheme.values()) {
            for (PointerDirection direction : PointerDirection.values()) {
                for (float percent : percents) {
                    FlyoutRenderer renderer = new FlyoutRenderer();
                    if (theme == SpriteTheme.DARK) {
                        renderer.dark();
                    }
                    renderer.shaded1px();
                    FlyoutRenderer.Scope scope = renderer.open();
                    scope.pointer(direction, percent);
                    scope.graphics(graphics);
                    scope.font(this.font);
                    scope.flexSize(FLEX_PADDING);
                    scope.text(Component.literal(("" + percent).replace("0.", ".")));
                    Vector2i size = scope.resolveSize();
                    int outerWidth = (size == null ? 24 : size.x) + flyoutMargin * 2;
                    int outerHeight = (size == null ? 16 : size.y) + flyoutMargin * 2;
                    if (cursorX + outerWidth + gap > this.width) {
                        cursorX = 8;
                        cursorY += rowHeight + gap;
                        rowHeight = 0;
                    }
                    scope.pos(cursorX + flyoutMargin, cursorY + flyoutMargin);
                    scope.render();
                    scope.close();
                    cursorX += outerWidth + gap;
                    rowHeight = Math.max(rowHeight, outerHeight);
                }
            }
        }
        if (!this.layoutChecked) {
            this.layoutChecked = true;
            this.check(
                cursorY + rowHeight <= this.height && cursorX <= this.width,
                "全部控件必须落在画面内，末行底部为 " + (cursorY + rowHeight) + "，画面高 " + this.height
            );
        }
        this.renderedFrames++;
    }


    private static FlyoutRenderer createFlyout(
        SpriteTheme theme,
        SpriteStyle style,
        SpriteType type,
        SpriteVariant variant,
        PointerDirection direction
    ) {
        FlyoutRenderer renderer = new FlyoutRenderer();
        if (theme == SpriteTheme.DARK) {
            renderer.dark();
        }
        if (style == SpriteStyle.FLAT) {
            renderer.flat();
        }
        switch (type) {
            case SHADED_1PX -> renderer.shaded1px();
            case SHADED_2PX -> renderer.shaded2px();
            case NON_SHADED -> {
            }
        }
        switch (variant) {
            case HIGHLIGHTED -> renderer.highlighted();
            case HIGHLIGHTED_PRESSED -> renderer.highlightedPressed();
            case HIGHLIGHTED_PRESSING -> renderer.highlightedPressing();
            case PRESSED -> renderer.pressed();
            case NORMAL -> {
            }
        }
        return renderer;
    }

    private static String shortLabel(String label) {
        return label
            .replace("HIGHLIGHTED_PRESSING", "HL_PRESSING")
            .replace("HIGHLIGHTED_PRESSED", "HL_PRESS")
            .replace("HIGHLIGHTED", "HL")
            .replace("PRESSED", "P")
            .replace("NON_SHADED", "N")
            .replace("SHADED_1PX", "S1")
            .replace("SHADED_2PX", "S2");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 取本次核对发现的失败项，用于自动化入口在退出前写结果。
     */
    public List<String> failures() {
        return List.copyOf(this.failures);
    }

    public int checkCount() {
        return this.checks;
    }

    /**
     * 取已经完成的绘制帧数，供自动化入口等待画面稳定后再截图。
     */
    public int renderedFrames() {
        return this.renderedFrames;
    }

    /**
     * 可在不影响真实鼠标状态的前提下伪造“允许键被按住”的按钮渲染器，供链路测试使用。
     */
    private static final class PressedButtonRenderer extends ButtonRenderer {
        @Override
        protected boolean isMousePressed(IntSet allowedKeys) {
            return true;
        }
    }

    private void check(boolean condition, String message) {
        this.checks++;
        if (!condition) {
            this.failures.add(message);
        }
    }
}
