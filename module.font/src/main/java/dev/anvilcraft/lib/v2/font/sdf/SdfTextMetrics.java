package dev.anvilcraft.lib.v2.font.sdf;

import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.awt.Font;
import java.util.List;

/** Shared glyph advances and styled line breaking for measurement and rendering. */
@ApiStatus.Internal
public final class SdfTextMetrics {
    private final Font font;
    private final SdfGlyphAtlas[] atlases = new SdfGlyphAtlas[4];
    private final StringSplitter splitter;

    public SdfTextMetrics(@Nullable Font font, int lineHeight) {
        this.font = font == null ? new Font("Dialog", Font.PLAIN, 64) : font;
        this.splitter = new StringSplitter((codepoint, style) -> {
            SdfGlyphAtlas atlas = this.atlas(style);
            return atlas.scaledAdvance(codepoint, lineHeight / (float) atlas.awtHeight());
        });
    }

    public static Font styledFont(@Nullable Font font, Style style) {
        Font base = font == null ? new Font("Dialog", Font.PLAIN, 64) : font;
        int mask = base.getStyle() | (style.isBold() ? Font.BOLD : 0) | (style.isItalic() ? Font.ITALIC : 0);
        return mask == base.getStyle() ? base : base.deriveFont(mask);
    }

    public SdfGlyphAtlas atlas(Style style) {
        int mask = (style.isBold() ? Font.BOLD : 0) | (style.isItalic() ? Font.ITALIC : 0);
        SdfGlyphAtlas atlas = this.atlases[mask];
        if (atlas == null) {
            atlas = SdfGlyphAtlas.getOrCreate(styledFont(this.font, style)).join();
            this.atlases[mask] = atlas;
        }
        return atlas;
    }

    public StringSplitter splitter() {
        return this.splitter;
    }

    public List<FormattedText> split(FormattedText text, int width) {
        List<FormattedText> lines = this.splitter.splitLines(text, width, Style.EMPTY);
        return lines.isEmpty() ? List.of(FormattedText.EMPTY) : lines;
    }
}
