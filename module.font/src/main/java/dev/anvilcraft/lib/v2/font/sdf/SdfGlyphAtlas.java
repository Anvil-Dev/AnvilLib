package dev.anvilcraft.lib.v2.font.sdf;

import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal glyph atlas cache for the SDF text pipeline.
 *
 * <p>Current stage focuses on atlas construction and metrics caching for printable ASCII.
 * Actual SDF shader sampling is wired in later steps.</p>
 */
public final class SdfGlyphAtlas {
    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    private static final int CHAR_COUNT = LAST_CHAR - FIRST_CHAR + 1;
    private static final int COLUMNS = 16;
    /** AWT system fonts report size 1; derive to a fixed rendering size for the atlas. */
    private static final int ATLAS_FONT_SIZE = 64;

    private static final Map<String, SdfGlyphAtlas> CACHE = new ConcurrentHashMap<>();

    private final String key;
    private final Font font;
    private final int cellSize;
    private final int padding;
    private final int paddedCellSize;
    private final int rows;
    private final float sdfRadius;
    private int awtAscent;
    private final BufferedImage atlasImage;
    private final Map<Character, GlyphInfo> glyphs;

    private SdfGlyphAtlas(String key, Font font) {
        this.key = key;
        this.font = font;
        this.cellSize = Math.max(24, font.getSize() + 12);
        this.sdfRadius = Math.max(12, font.getSize() * 0.25f);
        this.padding = Math.max(4, this.cellSize / 6);
        this.paddedCellSize = this.cellSize + 2 * this.padding;
        this.rows = (int) Math.ceil(CHAR_COUNT / (double) COLUMNS);
        this.atlasImage = new BufferedImage(this.paddedCellSize * COLUMNS, this.paddedCellSize * this.rows, BufferedImage.TYPE_INT_ARGB);
        this.glyphs = new HashMap<>();

        this.buildAsciiAtlas();
    }

    public static SdfGlyphAtlas getOrCreate(@Nullable Font font) {
        final Font resolved = resolveFont(font);
        String key = resolved.getFontName(Locale.ENGLISH)+ "#" + resolved.getStyle() + "#" + resolved.getSize();
        return CACHE.computeIfAbsent(key, _ -> new SdfGlyphAtlas(key, resolved));
    }

    private static Font resolveFont(@Nullable Font font) {
        if (font == null) {
            return new Font("Dialog", Font.PLAIN, ATLAS_FONT_SIZE);
        }
        if (font.getSize() < 4) {
            return font.deriveFont((float) ATLAS_FONT_SIZE);
        }
        return font;
    }

    public String key() {
        return this.key;
    }

    public @Nullable GlyphInfo glyph(char c) {
        return this.glyphs.get(c);
    }

    public Font font() {
        return this.font;
    }

    /** The point size used to render glyphs in this atlas, used as reference for screen scaling. */
    public int awtHeight() {
        return this.font.getSize();
    }

    /** AWT ascent (baseline-to-top distance) in atlas pixels. */
    public int awtAscent() {
        return this.awtAscent;
    }

    public int measureText(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            GlyphInfo glyph = this.glyph(text.charAt(i));
            width += glyph == null ? this.cellSize / 2 : glyph.advance();
        }
        return width;
    }

    public BufferedImage atlasImage() {
        return this.atlasImage;
    }

    private void buildAsciiAtlas() {
        Graphics2D graphics = this.atlasImage.createGraphics();
        try {
            graphics.setFont(this.font);
            graphics.setColor(new Color(0, 0, 0, 0));
            graphics.fillRect(0, 0, this.atlasImage.getWidth(), this.atlasImage.getHeight());

            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            FontMetrics metrics = graphics.getFontMetrics();
            this.awtAscent = metrics.getAscent();

            for (int code = FIRST_CHAR; code <= LAST_CHAR; code++) {
                char character = (char) code;
                int slot = code - FIRST_CHAR;
                int col = slot % COLUMNS;
                int row = slot / COLUMNS;

                // Inner cell position within the padded grid
                int padX = col * this.paddedCellSize;
                int padY = row * this.paddedCellSize;
                int innerX = padX + this.padding;
                int innerY = padY + this.padding;

                BufferedImage glyphMask = new BufferedImage(this.cellSize, this.cellSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D maskGraphics = glyphMask.createGraphics();
                try {
                    maskGraphics.setFont(this.font);
                    maskGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    maskGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    maskGraphics.setColor(new Color(0, 0, 0, 0));
                    maskGraphics.fillRect(0, 0, this.cellSize, this.cellSize);
                    maskGraphics.setColor(Color.WHITE);
                    maskGraphics.drawString(String.valueOf(character), 2, Math.min(this.cellSize - 4, metrics.getAscent() + 2));
                } finally {
                    maskGraphics.dispose();
                }

                this.blitSdfGlyph(glyphMask, innerX, innerY);

                this.glyphs.put(character, new GlyphInfo(character, innerX, innerY, this.cellSize, this.cellSize, metrics.charWidth(character)));
            }
        } finally {
            graphics.dispose();
        }

        fillPadding();
    }

    /** Stretch edge pixels of each inner cell into the padding zone to prevent LINEAR sampling bleed. */
    private void fillPadding() {
        for (int code = FIRST_CHAR; code <= LAST_CHAR; code++) {
            int slot = code - FIRST_CHAR;
            int col = slot % COLUMNS;
            int row = slot / COLUMNS;

            int padX = col * this.paddedCellSize;
            int padY = row * this.paddedCellSize;
            int innerX0 = padX + this.padding;
            int innerY0 = padY + this.padding;
            int innerX1 = innerX0 + this.cellSize - 1;
            int innerY1 = innerY0 + this.cellSize - 1;

            for (int y = padY; y < padY + this.paddedCellSize; y++) {
                for (int x = padX; x < padX + this.paddedCellSize; x++) {
                    if (x >= innerX0 && x <= innerX1 && y >= innerY0 && y <= innerY1) {
                        continue;
                    }
                    int srcX = Math.clamp(x, innerX0, innerX1);
                    int srcY = Math.clamp(y, innerY0, innerY1);
                    this.atlasImage.setRGB(x, y, this.atlasImage.getRGB(srcX, srcY));
                }
            }
        }
    }

    private void blitSdfGlyph(BufferedImage glyphMask, int atlasX, int atlasY) {
        int w = this.cellSize;
        float maxRadius = this.sdfRadius;

        boolean[] inside = new boolean[w * w];
        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                inside[y * w + x] = isInside(glyphMask, x, y);
            }
        }

        float[] dist = computeEdt(inside, w, maxRadius);

        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                float signed = inside[i] ? dist[i] : -dist[i];
                float normalized = 0.5f + (signed / (2.0f * maxRadius));
                normalized = Math.max(0.0f, Math.min(1.0f, normalized));
                int channel = Math.round(normalized * 255.0f);
                int rgba = (channel << 24) | (channel << 16) | (channel << 8) | channel;
                this.atlasImage.setRGB(atlasX + x, atlasY + y, rgba);
            }
        }
    }

    /** Dead Reckoning Euclidean Distance Transform – O(n²) per cell. */
    private static float[] computeEdt(boolean[] inside, int w, float maxRadius) {
        int n = w * w;
        int HUGE = w * 3;
        int[] dx = new int[n];
        int[] dy = new int[n];

        // Initialize: edge pixels have (0,0), others have HUGE
        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                boolean edge = false;
                for (int ny = Math.max(0, y - 1); ny <= Math.min(w - 1, y + 1) && !edge; ny++) {
                    for (int nx = Math.max(0, x - 1); nx <= Math.min(w - 1, x + 1); nx++) {
                        if (nx == x && ny == y) continue;
                        if (inside[ny * w + nx] != inside[i]) {
                            edge = true;
                            break;
                        }
                    }
                }
                dx[i] = edge ? 0 : HUGE;
                dy[i] = edge ? 0 : HUGE;
            }
        }

        // Pass 1: top-left → bottom-right
        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (y > 0) {
                    if (x > 0)     tryUpdate(dx, dy, i, (y - 1) * w + (x - 1), x, y, x - 1, y - 1);
                    tryUpdate(dx, dy, i, (y - 1) * w + x, x, y, x, y - 1);
                    if (x < w - 1) tryUpdate(dx, dy, i, (y - 1) * w + (x + 1), x, y, x + 1, y - 1);
                }
                if (x > 0) tryUpdate(dx, dy, i, y * w + (x - 1), x, y, x - 1, y);
            }
        }

        // Pass 2: bottom-right → top-left
        for (int y = w - 1; y >= 0; y--) {
            for (int x = w - 1; x >= 0; x--) {
                int i = y * w + x;
                if (x < w - 1) tryUpdate(dx, dy, i, y * w + (x + 1), x, y, x + 1, y);
                if (y < w - 1) {
                    if (x > 0)     tryUpdate(dx, dy, i, (y + 1) * w + (x - 1), x, y, x - 1, y + 1);
                    tryUpdate(dx, dy, i, (y + 1) * w + x, x, y, x, y + 1);
                    if (x < w - 1) tryUpdate(dx, dy, i, (y + 1) * w + (x + 1), x, y, x + 1, y + 1);
                }
            }
        }

        float[] dist = new float[n];
        for (int i = 0; i < n; i++) {
            dist[i] = Math.min((float) Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]), maxRadius);
        }
        return dist;
    }

    private static void tryUpdate(int[] dx, int[] dy, int cur, int nbr, int cx, int cy, int nx, int ny) {
        int ndx = dx[nbr] + (nx - cx);
        int ndy = dy[nbr] + (ny - cy);
        int nd2 = ndx * ndx + ndy * ndy;
        int od2 = dx[cur] * dx[cur] + dy[cur] * dy[cur];
        if (nd2 < od2) {
            dx[cur] = ndx;
            dy[cur] = ndy;
        }
    }

    private static boolean isInside(BufferedImage image, int x, int y) {
        int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
        return alpha > 16;
    }

    public record GlyphInfo(char value, int atlasX, int atlasY, int width, int height, int advance) {
        public int endX() {
            return this.atlasX + this.width;
        }

        public int endY() {
            return this.atlasY + this.height;
        }
    }
}

