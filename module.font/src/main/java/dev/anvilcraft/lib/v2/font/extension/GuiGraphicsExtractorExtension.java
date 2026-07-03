package dev.anvilcraft.lib.v2.font.extension;

import com.google.common.annotations.Beta;
import dev.anvilcraft.lib.v2.font.sdf.SdfTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.awt.Font;

@Beta
public interface GuiGraphicsExtractorExtension {
    default GuiGraphicsExtractor self() {
        return (GuiGraphicsExtractor) this;
    }

    default SdfTextRenderer anvillib$textRenderer() {
        throw new AssertionError("Not implemented!");
    }

    default void anvillib$text(Font font, @Nullable String str, int x, int y, int color) {
        this.anvillib$text(font, str, x, y, color, false);
    }

    default void anvillib$text(Font font, @Nullable String str, int x, int y, int color, boolean dropShadow) {
        this.anvillib$textRenderer().drawString(this.self(), font, str, x, y, color, dropShadow);
    }

    default void anvillib$text(Font font, FormattedCharSequence str, int x, int y, int color) {
        this.anvillib$text(font, str, x, y, color, false);
    }

    default void anvillib$text(Font font, FormattedCharSequence str, int x, int y, int color, boolean dropShadow) {
        this.anvillib$textRenderer().drawFormatted(this.self(), font, str, x, y, color, dropShadow);
    }

    default void anvillib$text(Font font, Component str, int x, int y, int color) {
        this.anvillib$text(font, str, x, y, color, false);
    }

    default void anvillib$text(Font font, Component str, int x, int y, int color, boolean dropShadow) {
        this.anvillib$textRenderer().drawComponent(this.self(), font, str, x, y, color, dropShadow);
    }

    default void anvillib$centeredText(Font font, String str, int x, int y, int color) {
        this.anvillib$centeredText(font, Component.literal(str), x, y, color);
    }

    default void anvillib$centeredText(Font font, Component text, int x, int y, int color) {
        this.anvillib$textRenderer().drawCentered(this.self(), font, text, x, y, color);
    }

    default void anvillib$centeredText(Font font, FormattedCharSequence text, int x, int y, int color) {
        this.anvillib$textRenderer().drawCentered(this.self(), font, text, x, y, color);
    }

    default void anvillib$textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col) {
        this.anvillib$textWithWordWrap(font, string, x, y, width, col, false);
    }

    default void anvillib$textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col, boolean dropShadow) {
        this.anvillib$textRenderer().drawWrapped(this.self(), font, string, x, y, width, col, dropShadow);
    }

    default void anvillib$textWithBackdrop(Font font, Component str, int textX, int textY, int textWidth, int textColor) {
        GuiGraphicsExtractor graphics = this.self();
        int backgroundColor = Minecraft.getInstance().options.getBackgroundColor(0.0F);
        if (backgroundColor != 0) {
            int padding = 2;
            graphics.fill(
                textX - padding,
                textY - padding,
                textX + textWidth + padding,
                textY + 9 + padding,
                ARGB.multiply(backgroundColor, textColor)
            );
        }
        this.anvillib$text(font, str, textX, textY, textColor, true);
    }
}
