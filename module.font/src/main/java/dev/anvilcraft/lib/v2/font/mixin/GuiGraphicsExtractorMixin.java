package dev.anvilcraft.lib.v2.font.mixin;

import dev.anvilcraft.lib.v2.font.extension.GuiGraphicsExtractorExtension;
import dev.anvilcraft.lib.v2.font.sdf.SdfTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.awt.Font;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin implements GuiGraphicsExtractorExtension {
    @Override
    public void anvillib$text(Font font, @Nullable String str, int x, int y, int color) {
        this.anvillib$text(font, str, x, y, color, false);
    }

    @Override
    public void anvillib$text(Font font, @Nullable String str, int x, int y, int color, boolean dropShadow) {
        SdfTextRenderer.drawString(this.self(), font, str, x, y, color, dropShadow);
    }

    @Override
    public void anvillib$text(Font font, FormattedCharSequence str, int x, int y, int color) {
        this.anvillib$text(font, str, x, y, color, false);
    }

    @Override
    public void anvillib$text(Font font, FormattedCharSequence str, int x, int y, int color, boolean dropShadow) {
        SdfTextRenderer.drawFormatted(this.self(), font, str, x, y, color, dropShadow);
    }

    @Override
    public void anvillib$text(Font font, Component str, int x, int y, int color) {
        this.anvillib$text(font, str, x, y, color, false);
    }

    @Override
    public void anvillib$text(Font font, Component str, int x, int y, int color, boolean dropShadow) {
        SdfTextRenderer.drawComponent(this.self(), font, str, x, y, color, dropShadow);
    }

    @Override
    public void anvillib$centeredText(Font font, String str, int x, int y, int color) {
        this.anvillib$centeredText(font, Component.literal(str), x, y, color);
    }

    @Override
    public void anvillib$centeredText(Font font, Component text, int x, int y, int color) {
        SdfTextRenderer.drawCentered(this.self(), font, text, x, y, color);
    }

    @Override
    public void anvillib$centeredText(Font font, FormattedCharSequence text, int x, int y, int color) {
        SdfTextRenderer.drawCentered(this.self(), font, text, x, y, color);
    }

    @Override
    public void anvillib$textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col) {
        this.anvillib$textWithWordWrap(font, string, x, y, width, col, false);
    }

    @Override
    public void anvillib$textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col, boolean dropShadow) {
        SdfTextRenderer.drawWrapped(this.self(), font, string, x, y, width, col, dropShadow);
    }

    @Override
    public void anvillib$textWithBackdrop(Font font, Component str, int textX, int textY, int textWidth, int textColor) {
        GuiGraphicsExtractor graphics = this.self();
        graphics.textWithBackdrop(Minecraft.getInstance().font, str, textX, textY, textWidth, textColor);
    }
}

