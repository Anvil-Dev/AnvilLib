package dev.anvilcraft.lib.v2.font.sdf;

import dev.anvilcraft.lib.v2.font.ALFont;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.awt.Font;
import java.util.List;
import java.util.Optional;

/** Executable regression checks; no game instance or OpenGL context is needed. */
public final class FontRegressionTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Font font = new Font("Dialog", Font.PLAIN, 64);
        for (int mask = 0; mask < 4; mask++) {
            Style style = Style.EMPTY.withBold((mask & Font.BOLD) != 0).withItalic((mask & Font.ITALIC) != 0);
            SdfTextMetrics metrics = new SdfTextMetrics(font, 9);
            SdfGlyphAtlas atlas = metrics.atlas(style);
            float scale = 9f / atlas.awtHeight();
            for (int cp = 32; cp <= 126; cp++) {
                for (int length : new int[]{1, 2, 20, 81}) {
                    String text = Character.toString(cp).repeat(length);
                    int width = (int) metrics.splitter().stringWidth(FormattedCharSequence.forward(text, style));
                    int rendered = SdfTextLayout.fromAtlas(atlas, text, 0, 0, scale).width();
                    check(width == rendered, "Measured and rendered widths differ: " + text);
                }
            }
        }

        SdfTextMetrics metrics = new SdfTextMetrics(font, 9);
        SdfGlyphAtlas atlas = metrics.atlas(Style.EMPTY);
        for (String text : List.of("iiiiiiiiiiiiiiiiiiii", "中文连续文本换行测试", "AB😀CD😀EF")) {
            for (int width : new int[]{0, 1, 10, 23, 100}) {
                List<FormattedText> lines = metrics.split(FormattedText.of(text), width);
                StringBuilder joined = new StringBuilder();
                for (FormattedText line : lines) {
                    String value = line.getString();
                    joined.append(value);
                    check(validSurrogates(value), "Split a surrogate pair");
                    check(metrics.splitter().stringWidth(line) <= width || value.codePointCount(0, value.length()) == 1,
                        "Line exceeds width with more than one codepoint");
                }
                check(joined.toString().equals(text), "Wrapping lost text");
            }
        }
        check(metrics.split(FormattedText.of(""), 10).size() == 1, "Empty text needs one line");
        check(metrics.split(FormattedText.of("a\n\nb\n"), 100).stream().map(FormattedText::getString).toList()
            .equals(List.of("a", "", "b", "")), "Explicit empty lines were lost");
        check(ALFont.wrapLines(atlas, "iiiiiiiiiiiiiiiiiiii", 10, 9f / atlas.awtHeight()).size() > 1,
            "Legacy wrap API did not wrap a long word");

        Style decorated = Style.EMPTY.withBold(true).withItalic(true).withUnderlined(true)
            .withStrikethrough(true).withColor(0x12AB34);
        FormattedText text = FormattedText.composite(FormattedText.of("AAAA", decorated), FormattedText.of("BBBB", Style.EMPTY));
        for (FormattedText line : metrics.split(text, 13)) {
            line.visit((style, value) -> {
                for (int cp : value.codePoints().toArray()) {
                    check(style.equals(cp == 'A' ? decorated : Style.EMPTY), "Wrapping changed style");
                }
                return Optional.empty();
            }, Style.EMPTY);
        }
        int styledWidth = (int) metrics.splitter().stringWidth(text);
        check(styledWidth == SdfTextLayout.fromAtlas(metrics.atlas(decorated), "AAAA", 0, 0,
                9f / metrics.atlas(decorated).awtHeight()).width()
            + SdfTextLayout.fromAtlas(atlas, "BBBB", 0, 0, 9f / atlas.awtHeight()).width(), "Styled width differs");

        for (Style style : List.of(Style.EMPTY, decorated)) {
            check(new SdfTextMetrics(null, 9).atlas(style) != null, "Null font fallback failed");
        }
        String unicode = "A😀B";
        for (int width = 0; width < 30; width++) {
            String head = metrics.splitter().plainHeadByWidth(unicode, width, Style.EMPTY);
            String tail = metrics.splitter().plainTailByWidth(unicode, width, Style.EMPTY);
            check(validSurrogates(head) && validSurrogates(tail), "Truncation split a surrogate pair");
            check(metrics.splitter().stringWidth(FormattedCharSequence.forward(head, Style.EMPTY)) <= width,
                "Truncated head exceeds width");
            check(metrics.splitter().stringWidth(FormattedCharSequence.forward(tail, Style.EMPTY)) <= width,
                "Truncated tail exceeds width");
        }

        SdfGlyphAtlas pending = SdfGlyphAtlas.getOrCreate(new Font("Dialog", Font.PLAIN, 63)).join();
        int before = pending.scaledAdvance('新', 9f / pending.awtHeight());
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (pending.glyph('新', page -> {}) == null && System.nanoTime() < deadline) Thread.sleep(5);
        check(pending.glyph('新', page -> {}) != null, "Glyph generation timed out");
        check(before == pending.scaledAdvance('新', 9f / pending.awtHeight()), "Width changed after glyph generation");
        System.out.println("FONT_REGRESSION_PASS " + checks);
    }

    private static boolean validSurrogates(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c)) return false;
        }
        return true;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
