package dev.anvilcraft.lib.v2.font.sdf;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CPU-side glyph layout output for SDF text rendering.
 */
public final class SdfTextLayout {
    private final List<GlyphQuad> quads;
    private final int width;
    private final int height;

    private SdfTextLayout(List<GlyphQuad> quads, int width, int height) {
        this.quads = quads;
        this.width = width;
        this.height = height;
    }

    public static SdfTextLayout from(SdfGlyphAtlas atlas, @Nullable String text, int x, int y) {
        return fromAtlas(atlas, text, x, y);
    }

    public static SdfTextLayout fromAtlas(SdfGlyphAtlas atlas, @Nullable String text, int x, int y) {
        if (text == null || text.isEmpty()) {
            return new SdfTextLayout(List.of(), 0, 0);
        }

        List<GlyphQuad> quads = new ArrayList<>();
        int penX = x;
        int maxHeight = 0;

        int atlasWidth = atlas.atlasImage().getWidth();
        int atlasHeight = atlas.atlasImage().getHeight();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            SdfGlyphAtlas.GlyphInfo glyph = atlas.glyph(c);
            if (glyph == null) {
                penX += Math.max(6, atlas.font().getSize() / 2);
                continue;
            }

            float u0 = glyph.atlasX() / (float) atlasWidth;
            float v0 = glyph.atlasY() / (float) atlasHeight;
            float u1 = glyph.endX() / (float) atlasWidth;
            float v1 = glyph.endY() / (float) atlasHeight;

            quads.add(new GlyphQuad(penX, y, penX + glyph.width(), y + glyph.height(), u0, v0, u1, v1, c));

            penX += Math.max(1, glyph.advance());
            maxHeight = Math.max(maxHeight, glyph.height());
        }

        return new SdfTextLayout(quads, Math.max(0, penX - x), maxHeight);
    }

    public static SdfTextLayout fromAsciiTexture(@Nullable String text, int x, int y, int glyphSize, int gridSize, int textureSize) {
        if (text == null || text.isEmpty()) {
            return new SdfTextLayout(List.of(), 0, 0);
        }

        List<GlyphQuad> quads = new ArrayList<>();
        int penX = x;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c > 126) {
                return new SdfTextLayout(List.of(), 0, 0);
            }

            int col = c % gridSize;
            int row = c / gridSize;

            float u0 = (col * glyphSize) / (float) textureSize;
            float v0 = (row * glyphSize) / (float) textureSize;
            float u1 = ((col + 1) * glyphSize) / (float) textureSize;
            float v1 = ((row + 1) * glyphSize) / (float) textureSize;

            quads.add(new GlyphQuad(penX, y, penX + glyphSize, y + glyphSize, u0, v0, u1, v1, c));
            penX += glyphSize;
        }

        return new SdfTextLayout(quads, Math.max(0, penX - x), glyphSize);
    }

    public List<GlyphQuad> quads() {
        return Collections.unmodifiableList(this.quads);
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public record GlyphQuad(
        int x0, int y0, int x1, int y1, float u0, float v0, float u1, float v1, char glyph
    ) {
    }
}

