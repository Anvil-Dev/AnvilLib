package dev.anvilcraft.lib.v2.font;

import com.google.common.annotations.Beta;
import dev.anvilcraft.lib.v2.font.sdf.SdfGlyphAtlas;
import dev.anvilcraft.lib.v2.font.sdf.SdfTextMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps {@link Font} with text measurement and layout utilities that
 * delegate to {@link SdfGlyphAtlas} for glyph metrics.
 */
@Beta
public class ALFont {
    public final int lineHeight = Minecraft.getInstance().font.lineHeight;
    private final Font font;
    private final SdfTextMetrics metrics;

    public ALFont(Font font) {
        this.font = font;
        this.metrics = new SdfTextMetrics(font, this.lineHeight);
    }

    public Font awtFont() { return this.font; }

    // ── Width measurement ───────────────────────────────────────

    public int width(String str) {
        return width(FormattedCharSequence.forward(str, Style.EMPTY));
    }

    public int width(FormattedText text) { return (int) this.metrics.splitter().stringWidth(text); }

    public int width(FormattedCharSequence text) { return (int) this.metrics.splitter().stringWidth(text); }

    // ── Substring by width ──────────────────────────────────────

    public String plainSubstrByWidth(String str, int maxWidth, boolean reverse) {
        if (reverse) return plainTailByWidth(str, maxWidth);
        return plainHeadByWidth(str, maxWidth);
    }

    public String plainSubstrByWidth(String str, int maxWidth) {
        return plainHeadByWidth(str, maxWidth);
    }

    private String plainHeadByWidth(String str, int maxWidth) {
        return this.metrics.splitter().plainHeadByWidth(str, maxWidth, Style.EMPTY);
    }

    private String plainTailByWidth(String str, int maxWidth) {
        return this.metrics.splitter().plainTailByWidth(str, maxWidth, Style.EMPTY);
    }

    public FormattedText substrByWidth(FormattedText text, int maxWidth) {
        return this.metrics.splitter().headByWidth(text, maxWidth, Style.EMPTY);
    }

    // ── Line splitting ──────────────────────────────────────────

    public List<FormattedCharSequence> split(FormattedText input, int maxWidth) {
        return Language.getInstance().getVisualOrder(this.metrics.split(input, maxWidth));
    }

    public int wordWrapHeight(FormattedText input, int maxWidth) {
        return lineHeight * split(input, maxWidth).size();
    }

    // ── Internal helpers ────────────────────────────────────────

    static String flatten(FormattedCharSequence text) {
        StringBuilder buf = new StringBuilder();
        text.accept((i, s, cp) -> { buf.appendCodePoint(cp); return true; });
        return buf.toString();
    }

    public static List<String> wrapLines(SdfGlyphAtlas atlas, String text, int maxWidth, float scale) {
        List<String> lines = new ArrayList<>();
        StringSplitter splitter = new StringSplitter((codepoint, style) -> atlas.scaledAdvance(codepoint, scale));
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) { lines.add(""); continue; }
            splitter.splitLines(para, maxWidth, Style.EMPTY, false,
                (style, start, end) -> lines.add(para.substring(start, end)));
        }
        return lines;
    }
}
