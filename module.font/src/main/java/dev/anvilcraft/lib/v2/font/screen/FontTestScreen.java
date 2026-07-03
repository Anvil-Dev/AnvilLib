package dev.anvilcraft.lib.v2.font.screen;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class FontTestScreen extends Screen {
    protected final Screen lastScreen;

    protected FontTestScreen(final Screen parent) {
        super(Component.literal("Font Test"));
        this.lastScreen = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int offsetX = this.width / 2;
        int offsetY = (this.height - this.font.lineHeight * 9) / 2;
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("正在准备Windows").withStyle(ChatFormatting.RED),
            offsetX,
            offsetY,
            0xFFFFFFFF
        );
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("请不要关闭你的计算机").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withStrikethrough(true)),
            offsetX,
            offsetY + this.font.lineHeight,
            0xFFFFFFFF
        );
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("中国传播家文化的主题餐厅").withStyle(Style.EMPTY.withBold(true)),
            offsetX,
            offsetY + this.font.lineHeight * 2,
            0xFFFFFFFF
        );
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("家是本  家是本心灵家港，幸福味道记忆处").withStyle(Style.EMPTY.withItalic(true)),
            offsetX,
            offsetY + this.font.lineHeight * 3,
            0xFFFFFFFF
        );
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("一群人，一辈子，干好传播家是本文化这件事")
                .withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.DARK_PURPLE)),
            offsetX,
            offsetY + this.font.lineHeight * 4,
            0xFFFFFFFF
        );
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("The quick brown fox jumped over the lazy dog.")
                .withStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.DARK_PURPLE).withUnderlined(true)),
            offsetX,
            offsetY + this.font.lineHeight * 5,
            0xFFFFFFFF
        );
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("隨手存取 AI，隨時使用最佳效能。善用 Windows 11 功能，進而保護並提升您的數碼生活。")
                .withStyle(Style.EMPTY.withItalic(true).withBold(true)),
            offsetX,
            offsetY + this.font.lineHeight * 6,
            0xFFFFFFFF
        );
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("1234567890/*-+!@#$%^&*()_+{}:\">?<[];\\,./'"),
            offsetX,
            offsetY + this.font.lineHeight * 7,
            0xFFFFFFFF
        );
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("【】；‘，。、《超かぐや姫！》？、|"),
            offsetX,
            offsetY + this.font.lineHeight * 8,
            0xFFFFFFFF
        );
        graphics.anvillib$centeredText(
            AnvilLibFont.getSelectFont(),
            Component.literal("混淆文字测试")
                .withStyle(Style.EMPTY.withItalic(true).withBold(true).withObfuscated(true)),
            offsetX,
            offsetY + this.font.lineHeight * 9,
            0xFFFFFFFF
        );
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
