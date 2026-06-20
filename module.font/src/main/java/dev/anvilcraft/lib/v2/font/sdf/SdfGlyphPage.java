package dev.anvilcraft.lib.v2.font.sdf;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A single 1024×1024 atlas page holding packed glyphs.
 */
@ApiStatus.Internal
public final class SdfGlyphPage {
    private static final int SIZE = SdfGlyphAtlas.PAGE_SIZE;
    final BufferedImage image;
    final AtomicInteger version = new AtomicInteger();
    final int cols, rows;
    int nextCol, nextRow;
    Identifier textureId;
    volatile boolean dirty = true;

    SdfGlyphPage(int paddedCellSize) {
        this.cols = SIZE / paddedCellSize;
        this.rows = SIZE / paddedCellSize;
        this.image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
    }

    boolean hasSpace() {
        return nextRow < rows;
    }

    synchronized SdfGlyphAtlas.GlyphEntry placeGlyph(SdfGlyphAtlas atlas, BufferedImage mask) {
        int col = nextCol, row = nextRow;
        int padX = col * atlas.paddedCellSize;
        int padY = row * atlas.paddedCellSize;
        int innerX = padX + atlas.padding;
        int innerY = padY + atlas.padding;
        SdfGlyphAtlas.blitSdfGlyph(mask, this.image, innerX, innerY, atlas.cellSize, atlas.sdfRadius);
        nextCol++;
        if (nextCol >= cols) {
            nextCol = 0;
            nextRow++;
        }
        dirty = true;
        return new SdfGlyphAtlas.GlyphEntry(0, innerX, innerY, atlas.cellSize, atlas.cellSize, 0);
    }

    synchronized void fillPaddingForCell(SdfGlyphAtlas atlas, SdfGlyphAtlas.GlyphEntry e) {
        int col = (e.atlasX() - atlas.padding) / atlas.paddedCellSize;
        int row = (e.atlasY() - atlas.padding) / atlas.paddedCellSize;
        SdfGlyphAtlas.fillPadding(
            this.image,
            col * atlas.paddedCellSize,
            row * atlas.paddedCellSize,
            atlas.paddedCellSize,
            atlas.cellSize,
            atlas.padding
        );
    }

    public void updateHash() {
        this.version.incrementAndGet();
    }
}
