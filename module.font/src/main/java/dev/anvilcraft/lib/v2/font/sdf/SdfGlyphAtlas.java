package dev.anvilcraft.lib.v2.font.sdf;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Multi-page on-demand SDF glyph atlas.
 * <p>
 * Metrics become available independently of rasterization. Glyphs are generated
 * on demand and packed into fixed-size 1024×1024 pages on background workers.
 */
@ApiStatus.Internal
@Slf4j
public final class SdfGlyphAtlas {
    static final int PAGE_SIZE = 1024;
    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    /**
     * AWT system fonts report size 1; derive to a fixed rendering size for the atlas.
     */
    private static final int ATLAS_FONT_SIZE = 64;

    private static final Map<String, CompletableFuture<SdfGlyphAtlas>> CACHE = new ConcurrentHashMap<>();

    // Metrics must never queue behind CPU-heavy glyph jobs: callers may join their future.
    private static final ExecutorService METRICS_EXECUTOR = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("AnvilLib-SDF-Metrics-", 0).factory()
    );
    private static final ExecutorService GLYPH_EXECUTOR = Executors.newFixedThreadPool(
        Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 2)),
        Thread.ofPlatform().daemon().name("AnvilLib-SDF-Glyph-", 0).factory()
    );

    private final String key;
    private final Font font;
    final int cellSize;
    final int padding;
    final int paddedCellSize;
    final float sdfRadius;
    private final int awtAscent;
    private final int awtHeight;
    private final FontMetrics fontMetrics;

    private final List<SdfGlyphPage> pages = new CopyOnWriteArrayList<>();
    private final Map<Integer, GlyphEntry> glyphMap = new ConcurrentHashMap<>();
    private final Set<Integer> pendingGlyphs = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Integer> advances = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Integer> glyphQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean workerScheduled = new AtomicBoolean();

    private SdfGlyphAtlas(String key, Font font) {
        this.key = key;
        this.font = font;
        log.info("SdfGlyphAtlas building for key={}, thread={}", key, Thread.currentThread().getName());

        // Capture font metrics first — needed by cellSize calculation
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

        // Cell must accommodate full ascent + descent above and below baseline,
        // plus 2px margin on each side. Previously used font.getSize()+12, which
        // could clip descenders or ascenders for fonts with large extents.
        int awtDescent = this.fontMetrics.getDescent();
        this.cellSize = Math.max(24, this.awtAscent + awtDescent + 4);
        this.sdfRadius = Math.max(12, font.getSize() * 0.25f);
        this.padding = Math.max(4, this.cellSize / 6);
        this.paddedCellSize = this.cellSize + 2 * this.padding;
        log.info("SdfGlyphAtlas cellSize={} ascent={} descent={} key={}", this.cellSize, this.awtAscent, awtDescent, key);

        log.info("SdfGlyphAtlas metrics ready for key={}, pages={}", key, this.pages.size());
    }

    // ── Public API ──────────────────────────────────────────────

    /**
     * Prepare font metrics on a background thread. Completion does not wait for
     * any glyph rasterization or GPU upload.
     */
    public static CompletableFuture<SdfGlyphAtlas> getOrCreate(@Nullable Font font) {
        Font resolved = resolveFont(font);
        String key = resolved.getFontName(Locale.ENGLISH) + "." + resolved.getStyle() + "." + resolved.getSize();
        return CACHE.computeIfAbsent(key, _ -> {
            log.debug("Starting SDF atlas build for key={}", key);
            return CompletableFuture.supplyAsync(() -> new SdfGlyphAtlas(key, resolved), METRICS_EXECUTOR);
        });
    }

    /**
     * Return the atlas once its metrics are ready, or {@code null} while preparing.
     * <p>
     * If the previous build failed (future completed exceptionally), the
     * failed future is removed and a fresh build is started so that a
     * transient error does not permanently disable the atlas.
     */
    public static @Nullable SdfGlyphAtlas getIfReady(@Nullable Font font) {
        Font resolved = resolveFont(font);
        String key = resolved.getFontName(Locale.ENGLISH) + "." + resolved.getStyle() + "." + resolved.getSize();
        CompletableFuture<SdfGlyphAtlas> f = CACHE.get(key);
        if (f != null && f.isDone()) {
            try {
                SdfGlyphAtlas atlas = f.get();
                return atlas;
            } catch (Exception e) {
                log.error("SDF atlas build failed for key={}, retrying", key, e);
                CACHE.remove(key, f);
                f = null;
            }
        }
        if (f == null) {
            log.info("No atlas future for key={}, starting build", key);
            getOrCreate(font);
        }
        return null;
    }

    private static Font resolveFont(@Nullable Font font) {
        if (font == null) return new Font("Dialog", Font.PLAIN, ATLAS_FONT_SIZE);
        if (font.getSize() < 4) return font.deriveFont((float) ATLAS_FONT_SIZE);
        return font;
    }

    public String key() {
        return this.key;
    }

    public Font font() {
        return this.font;
    }

    public int awtHeight() {
        return this.awtHeight;
    }

    public int awtAscent() {
        return this.awtAscent;
    }

    public int pageCount() {
        return this.pages.size();
    }

    public SdfGlyphPage page(int index) {
        return this.pages.get(index);
    }

    /**
     * Get the glyph entry for a codepoint. If the glyph is not yet in the
     * atlas, requests async creation and returns {@code null}. The caller
     * should handle the missing glyph gracefully (e.g. advance by a default
     * width); the glyph will be available on the next call.
     */
    public @Nullable GlyphEntry glyph(int codepoint, Consumer<SdfGlyphPage> pageConsumer) {
        GlyphEntry entry = this.glyphMap.get(codepoint);
        if (entry != null) return entry;
        // Trigger async creation on background thread
        if (this.pendingGlyphs.add(codepoint)) {
            this.glyphQueue.add(codepoint);
            scheduleWorker();
        }
        return null;
    }

    private void scheduleWorker() {
        if (this.workerScheduled.compareAndSet(false, true)) GLYPH_EXECUTOR.execute(this::drainGlyphs);
    }

    // One queued job per atlas, not one thread per character. No render-thread lock
    // is held during rasterization; only the completed pixels are published to a page.
    private void drainGlyphs() {
        try {
            Integer codepoint;
            while ((codepoint = this.glyphQueue.poll()) != null) {
                try {
                    if (!this.glyphMap.containsKey(codepoint)) createGlyph(codepoint);
                } catch (Exception e) {
                    log.error("Failed to create SDF glyph for codepoint {}", codepoint, e);
                } finally {
                    this.pendingGlyphs.remove(codepoint);
                }
            }
        } finally {
            this.workerScheduled.set(false);
            if (!this.glyphQueue.isEmpty()) scheduleWorker();
        }
    }

    public int measureText(String text) {
        return measureText(text, 0, text.length());
    }

    /**
     * Measure width of substring {@code text[start..end)} in atlas pixels.
     */
    public int measureText(String text, int start, int end) {
        int width = 0;
        for (int i = start; i < end; ) {
            int cp = text.codePointAt(i);
            width += measureCodepoint(cp);
            i += Character.charCount(cp);
        }
        return width;
    }

    /**
     * Measure a single codepoint's advance in atlas pixels.
     */
    public int measureCodepoint(int codepoint) {
        return this.advances.computeIfAbsent(codepoint, this.fontMetrics::charWidth);
    }

    /** Stable GUI advance, including while the glyph is being rasterized. */
    public int scaledAdvance(int codepoint, float scale) {
        return Math.max(1, Math.round(measureCodepoint(codepoint) * scale));
    }

    // ── Glyph creation ──────────────────────────────────────────

    private void createGlyph(int codepoint) {
        BufferedImage mask = renderMask(codepoint);
        BufferedImage cell = new BufferedImage(this.paddedCellSize, this.paddedCellSize, BufferedImage.TYPE_BYTE_GRAY);
        blitSdfGlyph(mask, cell, this.padding, this.padding, this.cellSize, this.sdfRadius);
        fillPadding(cell, 0, 0, this.paddedCellSize, this.cellSize, this.padding);

        int pageIdx = findOrCreatePageIndex();
        GlyphEntry placed = this.pages.get(pageIdx).placeGlyph(this, cell);
        // Publish only after the complete cell (including padding and upload revision) is visible.
        this.glyphMap.put(codepoint, new GlyphEntry(pageIdx, placed.atlasX(), placed.atlasY(),
            placed.width(), placed.height(), measureCodepoint(codepoint)));
        // Complete layouts contain only immutable glyph positions, so new glyphs do not invalidate them.
    }

    /** Queue common UI glyphs without delaying callers that only need metrics. */
    public void preWarmAscii() {
        for (int code = FIRST_CHAR; code <= LAST_CHAR; code++) glyph(code, ignored -> {});
    }

    private int findOrCreatePageIndex() {
        for (int i = 0; i < this.pages.size(); i++) {
            if (this.pages.get(i).hasSpace()) return i;
        }
        this.pages.add(new SdfGlyphPage(this.paddedCellSize));
        return this.pages.size() - 1;
    }

    /**
     * Render a single glyph as a white-on-transparent mask.
     */
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
            g.drawString(s, 2, this.awtAscent + 2);
        } finally {
            g.dispose();
        }
        return mask;
    }

    // ── Static SDF computation utilities ─────────────────────────

    static void blitSdfGlyph(BufferedImage mask, BufferedImage target, int tx, int ty, int w, float maxRadius) {
        boolean[] inside = new boolean[w * w];
        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                inside[y * w + x] = isInside(mask, x, y);
            }
        }

        // Distance to nearest outside pixel (for inside pixels)
        float[] distOutside = computeDistTo(inside, w, false);
        // Distance to nearest inside pixel (for outside pixels)
        float[] distInside = computeDistTo(inside, w, true);

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

    /**
     * Dead Reckoning EDT: distance from each pixel to nearest pixel where {@code inside == target}.
     */
    private static float[] computeDistTo(boolean[] inside, int w, boolean target) {
        int n = w * w, HUGE = w * 3;
        int[] dx = new int[n], dy = new int[n];
        for (int i = 0; i < n; i++) {
            dx[i] = (inside[i] == target) ? 0 : HUGE;
            dy[i] = (inside[i] == target) ? 0 : HUGE;
        }
        // Pass 1
        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (y > 0) {
                    if (x > 0) tryUpdate(dx, dy, i, (y - 1) * w + (x - 1), x, y, x - 1, y - 1);
                    tryUpdate(dx, dy, i, (y - 1) * w + x, x, y, x, y - 1);
                    if (x < w - 1) tryUpdate(dx, dy, i, (y - 1) * w + (x + 1), x, y, x + 1, y - 1);
                }
                if (x > 0) tryUpdate(dx, dy, i, y * w + (x - 1), x, y, x - 1, y);
            }
        }
        // Pass 2
        for (int y = w - 1; y >= 0; y--) {
            for (int x = w - 1; x >= 0; x--) {
                int i = y * w + x;
                if (x < w - 1) tryUpdate(dx, dy, i, y * w + (x + 1), x, y, x + 1, y);
                if (y < w - 1) {
                    if (x > 0) tryUpdate(dx, dy, i, (y + 1) * w + (x - 1), x, y, x - 1, y + 1);
                    tryUpdate(dx, dy, i, (y + 1) * w + x, x, y, x, y + 1);
                    if (x < w - 1) tryUpdate(dx, dy, i, (y + 1) * w + (x + 1), x, y, x + 1, y + 1);
                }
            }
        }
        float[] dist = new float[n];
        for (int i = 0; i < n; i++) {
            dist[i] = (float) Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]);
        }
        return dist;
    }

    private static void tryUpdate(int[] dx, int[] dy, int cur, int nbr, int cx, int cy, int nx, int ny) {
        int ndx = dx[nbr] + (nx - cx), ndy = dy[nbr] + (ny - cy);
        if (ndx * ndx + ndy * ndy < dx[cur] * dx[cur] + dy[cur] * dy[cur]) {
            dx[cur] = ndx;
            dy[cur] = ndy;
        }
    }

    private static boolean isInside(BufferedImage image, int x, int y) {
        return ((image.getRGB(x, y) >>> 24) & 0xFF) > 16;
    }

    static void fillPadding(BufferedImage img, int padX, int padY, int paddedCellSize, int cellSize, int padding) {
        int ix0 = padX + padding, iy0 = padY + padding;
        int ix1 = ix0 + cellSize - 1, iy1 = iy0 + cellSize - 1;
        for (int y = padY; y < padY + paddedCellSize; y++) {
            for (int x = padX; x < padX + paddedCellSize; x++) {
                if (x >= ix0 && x <= ix1 && y >= iy0 && y <= iy1) continue;
                img.setRGB(x, y, img.getRGB(Math.clamp(x, ix0, ix1), Math.clamp(y, iy0, iy1)));
            }
        }
    }

    // ── Inner types ─────────────────────────────────────────────

    public record GlyphEntry(int pageIndex, int atlasX, int atlasY, int width, int height, int advance) {
        public int endX() {
            return this.atlasX + this.width;
        }

        public int endY() {
            return this.atlasY + this.height;
        }
    }
}
