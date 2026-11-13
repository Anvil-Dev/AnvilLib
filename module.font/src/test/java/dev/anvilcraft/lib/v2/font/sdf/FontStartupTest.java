package dev.anvilcraft.lib.v2.font.sdf;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class FontStartupTest {
    private static SdfGlyphAtlas atlas(int size) throws Exception {
        return SdfGlyphAtlas.getOrCreate(new Font("Dialog", Font.PLAIN, size)).get(30, TimeUnit.SECONDS);
    }

    private static SdfGlyphAtlas.GlyphEntry awaitGlyph(SdfGlyphAtlas atlas, int cp) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        SdfGlyphAtlas.GlyphEntry glyph;
        while ((glyph = atlas.glyph(cp, ignored -> {})) == null) {
            assertTrue(System.nanoTime() < deadline, "glyph never became ready: " + cp);
            Thread.sleep(1);
        }
        return glyph;
    }

    @Test
    void measurementDoesNotRasterizeAndStaysStable() throws Exception {
        SdfGlyphAtlas atlas = atlas(63);
        assertEquals(0, atlas.pageCount(), "metrics must not wait for ASCII rasterization");
        String text = "Handbook 铁砧工艺 𝄞";
        int width = atlas.measureText(text);
        assertTrue(width > 0);
        assertEquals(0, atlas.pageCount(), "measuring a document must not generate its glyphs");
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            var metrics = graphics.getFontMetrics(atlas.font());
            for (int cp : text.codePoints().toArray()) {
                assertEquals(metrics.charWidth(cp), atlas.measureCodepoint(cp));
                assertEquals(atlas.measureCodepoint(cp), awaitGlyph(atlas, cp).advance());
            }
        } finally { graphics.dispose(); }
        assertEquals(width, atlas.measureText(text));
    }

    @Test
    void layoutHasStableAdvanceAndDoesNotCacheAnUnuploadedPage() throws Exception {
        SdfGlyphAtlas atlas = atlas(62);
        String text = "Wi中文";
        float scale = 9f / atlas.awtHeight();
        var pending = SdfTextLayout.fromAtlas(atlas, text, 0, 0, scale);
        for (int cp : text.codePoints().toArray()) awaitGlyph(atlas, cp);
        var notUploaded = SdfTextLayout.fromAtlas(atlas, text, 0, 0, scale);
        assertEquals(pending.width(), notUploaded.width());
        atlas.page(0).textureId = Identifier.fromNamespaceAndPath("test", "font");
        var uploaded = SdfTextLayout.fromAtlas(atlas, text, 0, 0, scale);
        assertNotSame(notUploaded, uploaded);
        assertNotNull(uploaded.pages().getFirst().atlasTexture());
        assertSame(uploaded, SdfTextLayout.fromAtlas(atlas, text, 0, 0, scale));
        awaitGlyph(atlas, '新');
        assertSame(uploaded, SdfTextLayout.fromAtlas(atlas, text, 0, 0, scale),
            "new immutable glyphs must not evict already complete text layouts");
    }

    @Test
    void partialUploadsPreservePixelsAndAConcurrentPublication() throws Exception {
        SdfGlyphAtlas atlas = atlas(61);
        awaitGlyph(atlas, 'A');
        SdfGlyphPage page = atlas.page(0);
        var first = page.snapshot(true);
        assertNotNull(first);
        assertEquals(1024 * 1024, first.pixels().length);
        page.uploaded(first);
        assertNull(page.snapshot(false));

        awaitGlyph(atlas, '中');
        var patch = page.snapshot(false);
        assertNotNull(patch);
        assertEquals(atlas.paddedCellSize * atlas.paddedCellSize, patch.pixels().length);
        assertTrue(patch.pixels().length < first.pixels().length / 16);
        for (int y = 0; y < patch.height(); y++) {
            for (int x = 0; x < patch.width(); x++) {
                assertEquals(page.image.getRGB(x + patch.x(), y + patch.y()) & 255,
                    patch.pixels()[y * patch.width() + x] & 255);
            }
        }
        byte[] saved = patch.pixels().clone();
        awaitGlyph(atlas, 'B');
        page.uploaded(patch);
        assertTrue(page.dirty, "upload acknowledgement must not lose a newer glyph");
        assertArrayEquals(saved, patch.pixels(), "upload snapshot must be immutable");
        var newer = page.snapshot(false);
        assertTrue(newer.version() > patch.version());
        page.uploaded(newer);
        assertFalse(page.dirty);
        assertNull(page.snapshot(false));
        assertNotNull(page.snapshot(true), "a recreated texture needs a full snapshot even when clean");
    }

    @Test
    void concurrentRequestsPublishEachCellOnceAcrossPages() throws Exception {
        SdfGlyphAtlas atlas = atlas(60);
        int[] codepoints = IntStream.range(32, 260).toArray();
        CompletableFuture<?>[] requests = IntStream.range(0, 8).mapToObj(ignored ->
            CompletableFuture.runAsync(() -> {
                for (int cp : codepoints) atlas.glyph(cp, page -> fail("render caller must not mutate pages"));
            })).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(requests).get(30, TimeUnit.SECONDS);
        var cells = new HashSet<String>();
        for (int cp : codepoints) {
            var glyph = awaitGlyph(atlas, cp);
            assertTrue(cells.add(glyph.pageIndex() + ":" + glyph.atlasX() + ":" + glyph.atlasY()));
        }
        assertTrue(atlas.pageCount() > 1);
        int revisions = 0;
        for (int i = 0; i < atlas.pageCount(); i++) revisions += atlas.page(i).version.get();
        assertEquals(codepoints.length, revisions, "duplicate requests must not consume extra cells");
    }
}
