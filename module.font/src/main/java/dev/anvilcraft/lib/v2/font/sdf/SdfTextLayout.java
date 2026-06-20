package dev.anvilcraft.lib.v2.font.sdf;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CPU-side glyph layout output for SDF text rendering, grouped by atlas page.
 * <p>
 * Layouts are cached by {@code (atlasKey, text, scale)} to avoid redundant
 * computation each frame. Quads are stored with positions relative to the
 * layout origin; the renderer supplies the screen-space offset.
 */
@ApiStatus.Internal
public final class SdfTextLayout {
    private static final int MAX_CACHE_SIZE = 1024;
    private static final Map<CacheKey, SdfTextLayout> LAYOUT_CACHE = new ConcurrentHashMap<>();

    private final List<PageQuads> pages;
    private final int width;
    private final int height;

    private SdfTextLayout(List<PageQuads> pages, int width, int height) {
        this.pages = pages;
        this.width = width;
        this.height = height;
    }

    /**
     * Compute glyph layout for a string. The returned quads have positions
     * relative to (0, 0); add {@code x}/{@code y} when rendering.
     * <p>
     * Result is cached when all glyphs are available in the atlas.
     */
    public static SdfTextLayout fromAtlas(SdfGlyphAtlas atlas, @Nullable String text, int x, int y, float scale) {
        if (text == null || text.isEmpty())
            return EMPTY;

        int scaleInt = Math.round(scale * 1000f);
        CacheKey key = new CacheKey(atlas.key(), text, scaleInt);
        SdfTextLayout cached = LAYOUT_CACHE.get(key);
        if (cached != null) return cached;

        SdfTextLayout layout = computeLayout(atlas, text, scale, scaleInt);
        if (layout != null && layout.allGlyphsAvailable) {
            evictIfNeeded();
            LAYOUT_CACHE.put(key, layout);
        }
        return layout;
    }

    /**
     * Invalidate cached layouts for a specific atlas (e.g. after glyph additions).
     */
    public static void invalidateAtlas(String atlasKey) {
        LAYOUT_CACHE.keySet().removeIf(k -> k.atlasKey.equals(atlasKey));
    }

    /**
     * Clear all cached layouts.
     */
    public static void clearCache() {
        LAYOUT_CACHE.clear();
    }

    private static void evictIfNeeded() {
        if (LAYOUT_CACHE.size() >= MAX_CACHE_SIZE) {
            // Evict half the entries (simple random-ish eviction via iterator)
            var it = LAYOUT_CACHE.keySet().iterator();
            int toRemove = MAX_CACHE_SIZE / 2;
            for (int i = 0; i < toRemove && it.hasNext(); i++) {
                it.next();
                it.remove();
            }
        }
    }

    @Nullable
    private static SdfTextLayout computeLayout(SdfGlyphAtlas atlas, String text, float scale, int scaleInt) {
        int penX = 0, maxHeight = 0;
        java.util.Map<Integer, List<GlyphQuad>> buckets = new java.util.LinkedHashMap<>();
        boolean allAvailable = true;

        Set<SdfGlyphPage> glyphPages = new HashSet<>();
        for (int ci = 0; ci < text.length(); ) {
            int cp = text.codePointAt(ci);
            ci += Character.charCount(cp);

            SdfGlyphAtlas.GlyphEntry glyph = atlas.glyph(cp, glyphPages::add);
            if (glyph == null) {
                allAvailable = false;
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
            // Positions relative to layout origin (0, 0)
            GlyphQuad quad = new GlyphQuad(penX, 0, penX + w, h, u0, v0, u1, v1, (char) cp);

            buckets.computeIfAbsent(glyph.pageIndex(), _ -> new ArrayList<>()).add(quad);
            penX += Math.max(1, Math.round(glyph.advance() * scale));
            maxHeight = Math.max(maxHeight, h);
        }
        glyphPages.forEach(SdfGlyphPage::updateHash);

        List<PageQuads> pages = new ArrayList<>();
        for (var entry : buckets.entrySet()) {
            int pi = entry.getKey();
            SdfGlyphPage page = atlas.page(pi);
            Identifier tex = page.textureId;
            pages.add(new PageQuads(pi, tex, page.image.getWidth(), page.image.getHeight(), entry.getValue()));
        }

        SdfTextLayout layout = new SdfTextLayout(pages, penX, maxHeight);
        layout.allGlyphsAvailable = allAvailable;
        return layout;
    }

    private boolean allGlyphsAvailable = true;

    private static final SdfTextLayout EMPTY = new SdfTextLayout(List.of(), 0, 0);

    public List<PageQuads> pages() { return Collections.unmodifiableList(this.pages); }
    public int width()  { return this.width; }
    public int height() { return this.height; }

    private record CacheKey(String atlasKey, String text, int scaleInt) {}

    public record PageQuads(int pageIndex, @Nullable Identifier atlasTexture,
                            int pageWidth, int pageHeight, List<GlyphQuad> quads) {}

    public record GlyphQuad(int x0, int y0, int x1, int y1,
                            float u0, float v0, float u1, float v1, char glyph) {}
}
