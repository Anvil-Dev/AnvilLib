package dev.anvilcraft.lib.v2.font.sdf;

import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
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
    private static final float SDF_RADIUS_PX = 12.0f;

    private static final Map<String, SdfGlyphAtlas> CACHE = new ConcurrentHashMap<>();

    private final String key;
    private final Font font;
    private final int cellSize;
    private final int rows;
    private final BufferedImage atlasImage;
    private final Map<Character, GlyphInfo> glyphs;

    private SdfGlyphAtlas(String key, Font font) {
        this.key = key;
        this.font = font;
        this.cellSize = Math.max(24, font.getSize() + 12);
        this.rows = (int) Math.ceil(CHAR_COUNT / (double) COLUMNS);
        this.atlasImage = new BufferedImage(this.cellSize * COLUMNS, this.cellSize * this.rows, BufferedImage.TYPE_INT_ARGB);
        this.glyphs = new HashMap<>();
        this.buildAsciiAtlas();
    }

    public static SdfGlyphAtlas getOrCreate(@Nullable Font font) {
        Font safeFont = font == null ? new Font("Dialog", Font.PLAIN, 16) : font;
        String key = safeFont.getFontName() + "#" + safeFont.getStyle() + "#" + safeFont.getSize();
        return CACHE.computeIfAbsent(key, _ -> new SdfGlyphAtlas(key, safeFont));
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

            for (int code = FIRST_CHAR; code <= LAST_CHAR; code++) {
                char character = (char) code;
                int slot = code - FIRST_CHAR;
                int col = slot % COLUMNS;
                int row = slot / COLUMNS;
                int x = col * this.cellSize;
                int y = row * this.cellSize;

                int baseline = y + Math.min(this.cellSize - 4, metrics.getAscent() + 2);
                int drawX = x + 2;

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

                this.blitSdfGlyph(glyphMask, x, y);

                this.glyphs.put(character, new GlyphInfo(character, x, y, this.cellSize, this.cellSize, metrics.charWidth(character)));
            }
        } finally {
            graphics.dispose();
        }
    }

    private void blitSdfGlyph(BufferedImage glyphMask, int atlasX, int atlasY) {
        for (int y = 0; y < this.cellSize; y++) {
            for (int x = 0; x < this.cellSize; x++) {
                boolean inside = isInside(glyphMask, x, y);
                float nearest = nearestEdgeDistance(glyphMask, x, y, inside, SDF_RADIUS_PX);
                float signed = inside ? nearest : -nearest;

                float normalized = 0.5f + (signed / (2.0f * SDF_RADIUS_PX));
                normalized = Math.max(0.0f, Math.min(1.0f, normalized));
                int channel = Math.round(normalized * 255.0f);
                int rgba = (channel << 24) | (channel << 16) | (channel << 8) | channel;
                this.atlasImage.setRGB(atlasX + x, atlasY + y, rgba);
            }
        }
    }

    private static boolean isInside(BufferedImage image, int x, int y) {
        int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
        return alpha > 16;
    }

    private static float nearestEdgeDistance(BufferedImage image, int px, int py, boolean inside, float maxRadius) {
        float best = maxRadius;
        int radius = (int) Math.ceil(maxRadius);

        int minX = Math.max(0, px - radius);
        int maxX = Math.min(image.getWidth() - 1, px + radius);
        int minY = Math.max(0, py - radius);
        int maxY = Math.min(image.getHeight() - 1, py + radius);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                boolean sampleInside = isInside(image, x, y);
                if (sampleInside == inside) {
                    continue;
                }
                float dx = x - px;
                float dy = y - py;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < best) {
                    best = dist;
                    if (best <= 0.5f) {
                        return best;
                    }
                }
            }
        }
        return best;
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

