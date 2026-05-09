package dev.anvilcraft.lib.v2.font.sdf;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CPU-side glyph layout output for SDF text rendering, grouped by atlas page.
 */
public final class SdfTextLayout {
    private final List<PageQuads> pages;
    private final int width;
    private final int height;

    private SdfTextLayout(List<PageQuads> pages, int width, int height) {
        this.pages = pages;
        this.width = width;
        this.height = height;
    }

    public static SdfTextLayout fromAtlas(SdfGlyphAtlas atlas, @Nullable String text, int x, int y, float scale) {
        if (text == null || text.isEmpty())
            return new SdfTextLayout(List.of(), 0, 0);

        int penX = x, maxHeight = 0, totalWidth = 0;
        java.util.Map<Integer, List<GlyphQuad>> buckets = new java.util.LinkedHashMap<>();

        for (int ci = 0; ci < text.length(); ) {
            int cp = text.codePointAt(ci);
            ci += Character.charCount(cp);

            SdfGlyphAtlas.GlyphEntry glyph = atlas.glyph(cp);
            if (glyph == null) {
                penX += Math.round(Math.max(6, atlas.font().getSize() / 2) * scale);
                continue;
            }
            if (glyph.width() <= 0) {
                penX += Math.max(1, Math.round(glyph.advance() * scale));
                continue;
            }

            SdfGlyphPage page = atlas.page(glyph.pageIndex());
            float u0 = glyph.atlasX() / (float) page.image.getWidth();
            float v0 = glyph.atlasY() / (float) page.image.getHeight();
            float u1 = glyph.endX() / (float) page.image.getWidth();
            float v1 = glyph.endY() / (float) page.image.getHeight();

            int w = Math.round(glyph.width() * scale);
            int h = Math.round(glyph.height() * scale);
            GlyphQuad quad = new GlyphQuad(penX, y, penX + w, y + h, u0, v0, u1, v1, (char) cp);

            buckets.computeIfAbsent(glyph.pageIndex(), _ -> new ArrayList<>()).add(quad);
            penX += Math.max(1, Math.round(glyph.advance() * scale));
            maxHeight = Math.max(maxHeight, h);
        }

        List<PageQuads> pages = new ArrayList<>();
        for (var entry : buckets.entrySet()) {
            int pi = entry.getKey();
            SdfGlyphPage page = atlas.page(pi);
            Identifier tex = page.textureId;
            pages.add(new PageQuads(pi, tex, page.image.getWidth(), page.image.getHeight(), entry.getValue()));
        }

        totalWidth = Math.max(0, penX - x);
        return new SdfTextLayout(pages, totalWidth, maxHeight);
    }

    public List<PageQuads> pages() { return Collections.unmodifiableList(this.pages); }
    public int width()  { return this.width; }
    public int height() { return this.height; }

    public record PageQuads(int pageIndex, @Nullable Identifier atlasTexture,
                            int pageWidth, int pageHeight, List<GlyphQuad> quads) {}

    public record GlyphQuad(int x0, int y0, int x1, int y1,
                            float u0, float v0, float u1, float v1, char glyph) {}
}
