package dev.anvilcraft.lib.v2.font.sdf;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

/** A page of immutable glyph cells, with short synchronized publication/snapshot operations. */
@ApiStatus.Internal
public final class SdfGlyphPage {
    private static final int SIZE = SdfGlyphAtlas.PAGE_SIZE;
    final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
    private final byte[] pixels = new byte[SIZE * SIZE];
    final AtomicInteger version = new AtomicInteger();
    final int cols, rows;
    int nextCol, nextRow;
    Identifier textureId;
    volatile boolean dirty = true;
    private int minX = SIZE, minY = SIZE, maxX, maxY;

    SdfGlyphPage(int paddedCellSize) {
        this.cols = SIZE / paddedCellSize;
        this.rows = SIZE / paddedCellSize;
    }

    boolean hasSpace() { return this.nextRow < this.rows; }

    SdfGlyphAtlas.GlyphEntry placeGlyph(SdfGlyphAtlas atlas, BufferedImage cell) {
        int size = atlas.paddedCellSize;
        // Preserve the gray color model's conversion, but perform it once on the worker.
        int[] colors = cell.getRGB(0, 0, size, size, null, 0, size);
        byte[] gray = new byte[colors.length];
        for (int i = 0; i < colors.length; i++) gray[i] = (byte) colors[i];
        synchronized (this) {
            int x = this.nextCol * size, y = this.nextRow * size;
            this.image.getRaster().setRect(x, y, cell.getRaster());
            for (int row = 0; row < size; row++) {
                System.arraycopy(gray, row * size, this.pixels, (y + row) * SIZE + x, size);
            }
            if (++this.nextCol >= this.cols) { this.nextCol = 0; this.nextRow++; }
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.maxX = Math.max(this.maxX, x + size);
            this.maxY = Math.max(this.maxY, y + size);
            this.version.incrementAndGet();
            this.dirty = true;
            return new SdfGlyphAtlas.GlyphEntry(0, x + atlas.padding, y + atlas.padding,
                atlas.cellSize, atlas.cellSize, 0);
        }
    }

    /** Compatibility hook for callers that explicitly invalidate a page. */
    public synchronized void updateHash() {
        this.minX = this.minY = 0;
        this.maxX = this.maxY = SIZE;
        this.version.incrementAndGet();
        this.dirty = true;
    }

    /** Copy only changed pixels; neither SDF computation nor native allocation holds this lock. */
    synchronized @Nullable Upload snapshot(boolean full) {
        if (!full && (!this.dirty || this.maxX <= this.minX || this.maxY <= this.minY)) return null;
        int x = full ? 0 : this.minX, y = full ? 0 : this.minY;
        int width = full ? SIZE : this.maxX - x, height = full ? SIZE : this.maxY - y;
        byte[] copy = new byte[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(this.pixels, (y + row) * SIZE + x, copy, row * width, width);
        }
        return new Upload(this.version.get(), x, y, width, height, copy);
    }

    synchronized void uploaded(Upload upload) {
        // A worker may have published another cell during the GPU upload. Keep it dirty.
        if (this.version.get() != upload.version()) return;
        this.minX = this.minY = SIZE;
        this.maxX = this.maxY = 0;
        this.dirty = false;
    }

    record Upload(int version, int x, int y, int width, int height, byte[] pixels) {}
}
