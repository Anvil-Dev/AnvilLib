package dev.anvilcraft.lib.v2.font;

import dev.anvilcraft.lib.v2.font.sdf.SdfGlyphAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps {@link Font} with text measurement and layout utilities that
 * delegate to {@link SdfGlyphAtlas} for glyph metrics.
 */
public class ALFont {
    public final int lineHeight = Minecraft.getInstance().font.lineHeight;
    private final Font font;

    public ALFont(Font font) {
        this.font = font;
    }

    public Font awtFont() { return this.font; }

    private SdfGlyphAtlas atlas() { return SdfGlyphAtlas.getOrCreate(this.font); }

    private float scale() { return (float) lineHeight / atlas().awtHeight(); }

    // ── Width measurement ───────────────────────────────────────

    public int width(String str) {
        return Mth.ceil(atlas().measureText(str) * scale());
    }

    public int width(FormattedText text) {
        return width(text.getString());
    }

    public int width(FormattedCharSequence text) {
        return width(flatten(text));
    }

    // ── Substring by width ──────────────────────────────────────

    public String plainSubstrByWidth(String str, int maxWidth, boolean reverse) {
        if (reverse) return plainTailByWidth(str, maxWidth);
        return plainHeadByWidth(str, maxWidth);
    }

    public String plainSubstrByWidth(String str, int maxWidth) {
        return plainHeadByWidth(str, maxWidth);
    }

    private String plainHeadByWidth(String str, int maxWidth) {
        SdfGlyphAtlas a = atlas();
        float s = scale();
        if (a.measureText(str) * s <= maxWidth) return str;
        int lo = 0, hi = str.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (a.measureText(str, 0, mid) * s <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return str.substring(0, lo);
    }

    private String plainTailByWidth(String str, int maxWidth) {
        SdfGlyphAtlas a = atlas();
        float s = scale();
        if (a.measureText(str) * s <= maxWidth) return str;
        int lo = 0, hi = str.length();
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (a.measureText(str, mid, str.length()) * s <= maxWidth) hi = mid;
            else lo = mid + 1;
        }
        return str.substring(lo);
    }

    public FormattedText substrByWidth(FormattedText text, int maxWidth) {
        return FormattedText.of(plainSubstrByWidth(text.getString(), maxWidth));
    }

    // ── Line splitting ──────────────────────────────────────────

    public List<FormattedCharSequence> split(FormattedText input, int maxWidth) {
        SdfGlyphAtlas a = atlas();
        float s = scale();
        List<String> lines = wrapLines(a, input.getString(), maxWidth, s);
        List<FormattedCharSequence> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(FormattedCharSequence.forward(line, Style.EMPTY));
        }
        return result;
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
        if (text.isEmpty()) { lines.add(""); return lines; }
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) { lines.add(""); continue; }
            String[] words = para.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String cand = line.isEmpty() ? word : line + " " + word;
                if (atlas.measureText(cand) * scale > maxWidth) {
                    if (line.isEmpty()) lines.add(word);
                    else { lines.add(line.toString()); line = new StringBuilder(word); }
                } else {
                    line = new StringBuilder(cand);
                }
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return lines;
    }
}
