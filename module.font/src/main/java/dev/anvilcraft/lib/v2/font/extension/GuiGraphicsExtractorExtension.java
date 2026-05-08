package dev.anvilcraft.lib.v2.font.extension;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.awt.Font;

public interface GuiGraphicsExtractorExtension {
    default GuiGraphicsExtractor self() {
        return (GuiGraphicsExtractor) this;
    }

    default void anvillib$text(Font font, @Nullable String str, int x, int y, int color) {
    }

    default void anvillib$text(Font font, @Nullable String str, int x, int y, int color, boolean dropShadow) {
    }

    default void anvillib$text(Font font, FormattedCharSequence str, int x, int y, int color) {
    }

    default void anvillib$text(Font font, FormattedCharSequence str, int x, int y, int color, boolean dropShadow) {
    }

    default void anvillib$text(Font font, Component str, int x, int y, int color) {
    }

    default void anvillib$text(Font font, Component str, int x, int y, int color, boolean dropShadow) {
    }

    default void anvillib$centeredText(Font font, String str, int x, int y, int color) {
    }

    default void anvillib$centeredText(Font font, Component text, int x, int y, int color) {
    }

    default void anvillib$centeredText(Font font, FormattedCharSequence text, int x, int y, int color) {
    }

    default void anvillib$textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col) {
    }

    default void anvillib$textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col, boolean dropShadow) {
    }

    default void anvillib$textWithBackdrop(Font font, Component str, int textX, int textY, int textWidth, int textColor) {
    }
}
