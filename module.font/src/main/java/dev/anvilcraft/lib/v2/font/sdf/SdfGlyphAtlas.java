package dev.anvilcraft.lib.v2.font.sdf;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-page on-demand SDF glyph atlas.
 * <p>
 * Glyphs are packed into fixed-size 1024×1024 pages. ASCII 32-126 is
 * pre-warmed; all other codepoints are rendered lazily on first use.
 */
public final class SdfGlyphAtlas {
    static final int PAGE_SIZE = 1024;
    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    /** AWT system fonts report size 1; derive to a fixed rendering size for the atlas. */
    private static final int ATLAS_FONT_SIZE = 64;

    private static final Map<String, SdfGlyphAtlas> CACHE = new ConcurrentHashMap<>();

    private final String key;
    private final Font font;
    final int cellSize;
    final int padding;
    final int paddedCellSize;
    final float sdfRadius;
    private int awtAscent;
    private int awtHeight;
    private final FontMetrics fontMetrics;

    private final List<SdfGlyphPage> pages = new ArrayList<>();
    private final Map<Integer, GlyphEntry> glyphMap = new HashMap<>();

    private SdfGlyphAtlas(String key, Font font) {
        this.key = key;
        this.font = font;
        this.cellSize = Math.max(24, font.getSize() + 12);
        this.sdfRadius = Math.max(12, font.getSize() * 0.25f);
        this.padding = Math.max(4, this.cellSize / 6);
        this.paddedCellSize = this.cellSize + 2 * this.padding;

        // Capture font metrics
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        try {
            g.setFont(this.font);
            this.fontMetrics = g.getFontMetrics();
            this.awtAscent = this.fontMetrics.getAscent();
            this.awtHeight = this.fontMetrics.getHeight();
        } finally {
            g.dispose();
        }

        preWarmAscii();
    }

    // ── Public API ──────────────────────────────────────────────

    public static SdfGlyphAtlas getOrCreate(@Nullable Font font) {
        Font resolved = resolveFont(font);
        String key = resolved.getFontName(Locale.ENGLISH) + "." + resolved.getStyle() + "." + resolved.getSize();
        return CACHE.computeIfAbsent(key, _ -> new SdfGlyphAtlas(key, resolved));
    }

    private static Font resolveFont(@Nullable Font font) {
        if (font == null) return new Font("Dialog", Font.PLAIN, ATLAS_FONT_SIZE);
        if (font.getSize() < 4) return font.deriveFont((float) ATLAS_FONT_SIZE);
        return font;
    }

    public String key() { return this.key; }

    public Font font() { return this.font; }

    public int awtHeight() { return this.font.getSize(); }

    public int awtAscent() { return this.awtAscent; }

    public int pageCount() { return this.pages.size(); }

    public SdfGlyphPage page(int index) { return this.pages.get(index); }

    /** Get (or lazily create) the glyph entry for a codepoint. */
    public @Nullable GlyphEntry glyph(int codepoint) {
        GlyphEntry entry = this.glyphMap.get(codepoint);
        if (entry != null) return entry;
        return createGlyph(codepoint);
    }

    public int measureText(String text) {
        return measureText(text, 0, text.length());
    }

    /** Measure width of substring {@code text[start..end)} in atlas pixels. */
    public int measureText(String text, int start, int end) {
        int width = 0;
        for (int i = start; i < end; ) {
            int cp = text.codePointAt(i);
            GlyphEntry g = glyph(cp);
            width += g == null ? this.cellSize / 2 : g.advance;
            i += Character.charCount(cp);
        }
        return width;
    }

    /** Measure a single codepoint's advance in atlas pixels. */
    public int measureCodepoint(int codepoint) {
        GlyphEntry g = glyph(codepoint);
        return g == null ? this.cellSize / 2 : g.advance;
    }

    // ── Glyph creation ──────────────────────────────────────────

    private @Nullable GlyphEntry createGlyph(int codepoint) {
        BufferedImage mask = renderMask(codepoint);
        if (mask == null) return null;

        int pageIdx = findOrCreatePageIndex();
        SdfGlyphPage page = this.pages.get(pageIdx);
        GlyphEntry entry = page.placeGlyph(this, mask);
        entry = new GlyphEntry(pageIdx, entry.atlasX(), entry.atlasY(),
            entry.width(), entry.height(),
            this.fontMetrics.charWidth(codepoint));
        this.glyphMap.put(codepoint, entry);
        page.fillPaddingForCell(this, entry);
        page.dirty = true;
        return entry;
    }

    private void preWarmAscii() {
        for (int code = FIRST_CHAR; code <= LAST_CHAR; code++) {
            createGlyph(code);
        }
    }

    private int findOrCreatePageIndex() {
        for (int i = 0; i < this.pages.size(); i++) {
            if (this.pages.get(i).hasSpace()) return i;
        }
        this.pages.add(new SdfGlyphPage(this.paddedCellSize));
        return this.pages.size() - 1;
    }

    /** Render a single glyph as a white-on-transparent mask. */
    private BufferedImage renderMask(int codepoint) {
        String s = new String(Character.toChars(codepoint));
        BufferedImage mask = new BufferedImage(this.cellSize, this.cellSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        try {
            g.setFont(this.font);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, this.cellSize, this.cellSize);
            g.setColor(Color.WHITE);
            g.drawString(s, 2, Math.min(this.cellSize - 4, this.awtAscent + 2));
        } finally {
            g.dispose();
        }
        return mask;
    }

    // ── Static SDF computation utilities ─────────────────────────

    static void blitSdfGlyph(BufferedImage mask, BufferedImage target, int tx, int ty, int w, float maxRadius) {
        boolean[] inside = new boolean[w * w];
        for (int y = 0; y < w; y++)
            for (int x = 0; x < w; x++)
                inside[y * w + x] = isInside(mask, x, y);

        // Distance to nearest outside pixel (for inside pixels)
        float[] distOutside = computeDistTo(inside, w, false);
        // Distance to nearest inside pixel (for outside pixels)
        float[] distInside  = computeDistTo(inside, w, true);

        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                float signed = inside[i] ? distOutside[i] : -distInside[i];
                signed = Math.min(signed, maxRadius);
                float normalized = 0.5f + (signed / (2.0f * maxRadius));
                int ch = Math.round(Math.max(0f, Math.min(1f, normalized)) * 255f);
                target.setRGB(tx + x, ty + y, (ch << 24) | (ch << 16) | (ch << 8) | ch);
            }
        }
    }

    /** Dead Reckoning EDT: distance from each pixel to nearest pixel where {@code inside == target}. */
    private static float[] computeDistTo(boolean[] inside, int w, boolean target) {
        int n = w * w, HUGE = w * 3;
        int[] dx = new int[n], dy = new int[n];
        for (int i = 0; i < n; i++) {
            dx[i] = (inside[i] == target) ? 0 : HUGE;
            dy[i] = (inside[i] == target) ? 0 : HUGE;
        }
        // Pass 1
        for (int y = 0; y < w; y++)
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (y > 0) {
                    if (x > 0)     tryUpdate(dx, dy, i, (y - 1) * w + (x - 1), x, y, x - 1, y - 1);
                    tryUpdate(dx, dy, i, (y - 1) * w + x, x, y, x, y - 1);
                    if (x < w - 1) tryUpdate(dx, dy, i, (y - 1) * w + (x + 1), x, y, x + 1, y - 1);
                }
                if (x > 0) tryUpdate(dx, dy, i, y * w + (x - 1), x, y, x - 1, y);
            }
        // Pass 2
        for (int y = w - 1; y >= 0; y--)
            for (int x = w - 1; x >= 0; x--) {
                int i = y * w + x;
                if (x < w - 1) tryUpdate(dx, dy, i, y * w + (x + 1), x, y, x + 1, y);
                if (y < w - 1) {
                    if (x > 0)     tryUpdate(dx, dy, i, (y + 1) * w + (x - 1), x, y, x - 1, y + 1);
                    tryUpdate(dx, dy, i, (y + 1) * w + x, x, y, x, y + 1);
                    if (x < w - 1) tryUpdate(dx, dy, i, (y + 1) * w + (x + 1), x, y, x + 1, y + 1);
                }
            }
        float[] dist = new float[n];
        for (int i = 0; i < n; i++)
            dist[i] = (float) Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]);
        return dist;
    }

    private static void tryUpdate(int[] dx, int[] dy, int cur, int nbr, int cx, int cy, int nx, int ny) {
        int ndx = dx[nbr] + (nx - cx), ndy = dy[nbr] + (ny - cy);
        if (ndx * ndx + ndy * ndy < dx[cur] * dx[cur] + dy[cur] * dy[cur]) {
            dx[cur] = ndx; dy[cur] = ndy;
        }
    }

    private static boolean isInside(BufferedImage image, int x, int y) {
        return ((image.getRGB(x, y) >>> 24) & 0xFF) > 16;
    }

    static void fillPadding(BufferedImage img, int padX, int padY, int paddedCellSize, int cellSize, int padding) {
        int ix0 = padX + padding, iy0 = padY + padding;
        int ix1 = ix0 + cellSize - 1, iy1 = iy0 + cellSize - 1;
        for (int y = padY; y < padY + paddedCellSize; y++)
            for (int x = padX; x < padX + paddedCellSize; x++) {
                if (x >= ix0 && x <= ix1 && y >= iy0 && y <= iy1) continue;
                img.setRGB(x, y, img.getRGB(Math.clamp(x, ix0, ix1), Math.clamp(y, iy0, iy1)));
            }
    }

    // ── Inner types ─────────────────────────────────────────────

    public record GlyphEntry(int pageIndex, int atlasX, int atlasY, int width, int height, int advance) {
        public int endX() { return this.atlasX + this.width; }
        public int endY() { return this.atlasY + this.height; }
    }
}

/** A single 1024×1024 atlas page holding packed glyphs. */
final class SdfGlyphPage {
    private static final int SIZE = SdfGlyphAtlas.PAGE_SIZE;
    final BufferedImage image;
    final int cols, rows;
    int nextCol, nextRow;
    Identifier textureId;
    boolean dirty = true;

    SdfGlyphPage(int paddedCellSize) {
        this.cols = SIZE / paddedCellSize;
        this.rows = SIZE / paddedCellSize;
        this.image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
    }

    boolean hasSpace() { return nextRow < rows; }

    SdfGlyphAtlas.GlyphEntry placeGlyph(SdfGlyphAtlas atlas, BufferedImage mask) {
        int col = nextCol, row = nextRow;
        int padX = col * atlas.paddedCellSize;
        int padY = row * atlas.paddedCellSize;
        int innerX = padX + atlas.padding;
        int innerY = padY + atlas.padding;
        SdfGlyphAtlas.blitSdfGlyph(mask, this.image, innerX, innerY, atlas.cellSize, atlas.sdfRadius);
        nextCol++;
        if (nextCol >= cols) { nextCol = 0; nextRow++; }
        dirty = true;
        return new SdfGlyphAtlas.GlyphEntry(0, innerX, innerY, atlas.cellSize, atlas.cellSize, 0);
    }

    void fillPaddingForCell(SdfGlyphAtlas atlas, SdfGlyphAtlas.GlyphEntry e) {
        int col = (e.atlasX() - atlas.padding) / atlas.paddedCellSize;
        int row = (e.atlasY() - atlas.padding) / atlas.paddedCellSize;
        SdfGlyphAtlas.fillPadding(this.image,
            col * atlas.paddedCellSize, row * atlas.paddedCellSize,
            atlas.paddedCellSize, atlas.cellSize, atlas.padding);
    }
}
