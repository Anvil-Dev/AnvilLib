package dev.anvilcraft.lib.v2.font.sdf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.anvilcraft.lib.v2.font.sdf.state.SdfTextRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.util.List;

/**
 * SDF text renderer that draws strings via the SDF text render pipeline.
 * <p>
 * Uses a CPU-generated SDF glyph atlas uploaded to a GPU texture,
 * sampled by a custom fragment shader for smooth anti-aliased text.
 */
public final class SdfTextRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdfTextRenderer.class);

    private final GpuSampler diffuseSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

    public SdfTextRenderer() {
    }

    public void drawString(
        GuiGraphicsExtractor graphics,
        @Nullable Font font,
        @Nullable String text,
        int x,
        int y,
        int color,
        boolean dropShadow
    ) {
        if (text == null || text.isEmpty()) return;

        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        float scale = scaleFor(atlas);
        int quadY = y - 2;
        SdfTextLayout layout = SdfTextLayout.fromAtlas(atlas, text, x, quadY, scale);
        if (layout.pages().isEmpty()) return;

        SdfAtlasTexture.ensureUploaded(atlas);
        for (SdfTextLayout.PageQuads pq : layout.pages()) {
            drawAtlasPipeline(graphics, pq, this.diffuseSampler, color);
        }
    }

    private static float scaleFor(SdfGlyphAtlas atlas) {
        return Minecraft.getInstance().font.lineHeight / (float) atlas.awtHeight();
    }

    /**
     * Derive a styled font for bold/italic.
     */
    private static Font styledFont(Font base, boolean bold, boolean italic) {
        int mask = Font.PLAIN;
        if (bold) mask |= Font.BOLD;
        if (italic) mask |= Font.ITALIC;
        return mask == Font.PLAIN ? base : base.deriveFont(mask);
    }

    /**
     * Replace codepoint with random ASCII for obfuscated style.
     */
    private static int obfuscateCodepoint(int codepoint, int index) {
        long t = System.currentTimeMillis() / 300L;
        int r = (int) (((long) index * 7L + t) % 95L);
        return 32 + r;
    }

    public void drawComponent(
        GuiGraphicsExtractor graphics,
        @Nullable Font font,
        Component text,
        int x,
        int y,
        int color,
        boolean dropShadow
    ) {
        this.drawFormatted(graphics, font, text.getVisualOrderText(), x, y, color, dropShadow);
    }

    public void drawFormatted(
        GuiGraphicsExtractor graphics,
        @Nullable Font font,
        FormattedCharSequence text,
        int x,
        int y,
        int color,
        boolean dropShadow
    ) {
        int[] pen = {
            x,
            x
        }; // pen[0] = current x, pen[1] = segment start x
        StringBuilder buf = new StringBuilder();
        int[] segColor = {color};
        boolean[] segBold = {false};
        boolean[] segItalic = {false};
        boolean[] segUnderline = {false};
        boolean[] segStrikethrough = {false};

        text.accept((index, style, codepoint) -> {
            if (style.isObfuscated()) {
                codepoint = obfuscateCodepoint(codepoint, index);
            }

            int c = colorFromStyle(style, color);
            boolean b = style.isBold();
            boolean i = style.isItalic();

            if ((c != segColor[0] || b != segBold[0] || i != segItalic[0]) && !buf.isEmpty()) {
                Font segFont = styledFont(font, segBold[0], segItalic[0]);
                pen[0] = flushFormattedSegment(graphics, segFont, buf.toString(), pen[0], y, segColor[0]);
                drawDecorations(graphics, pen[1], pen[0], y, segColor[0], segUnderline[0], segStrikethrough[0]);
                buf.setLength(0);
                pen[1] = pen[0];
            }

            buf.appendCodePoint(codepoint);
            segColor[0] = c;
            segBold[0] = b;
            segItalic[0] = i;
            segUnderline[0] = style.isUnderlined();
            segStrikethrough[0] = style.isStrikethrough();
            return true;
        });

        if (!buf.isEmpty()) {
            Font segFont = styledFont(font, segBold[0], segItalic[0]);
            pen[0] = flushFormattedSegment(graphics, segFont, buf.toString(), pen[0], y, segColor[0]);
            drawDecorations(graphics, pen[1], pen[0], y, segColor[0], segUnderline[0], segStrikethrough[0]);
        }
    }

    private int flushFormattedSegment(GuiGraphicsExtractor graphics, @Nullable Font font, String text, int x, int y, int color) {
        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        float scale = scaleFor(atlas);
        int quadY = y - 2;
        SdfTextLayout layout = SdfTextLayout.fromAtlas(atlas, text, x, quadY, scale);
        SdfAtlasTexture.ensureUploaded(atlas);
        for (SdfTextLayout.PageQuads pq : layout.pages()) {
            if (pq.atlasTexture() != null && !pq.quads().isEmpty()) {
                drawAtlasPipeline(graphics, pq, this.diffuseSampler, color);
            }
        }
        return x + layout.width();
    }

    /**
     * Draw underline and/or strikethrough lines relative to baseline.
     */
    private static void drawDecorations(
        GuiGraphicsExtractor graphics,
        int x0,
        int x1,
        int y,
        int color,
        boolean underline,
        boolean strikethrough
    ) {
        if (x1 <= x0) return;
        int lh = Minecraft.getInstance().font.lineHeight;
        if (strikethrough) {
            int sy = y + lh / 2;
            graphics.fill(x0, sy, x1, sy + 1, color);
        }
        if (underline) {
            int sy = y + lh;
            graphics.fill(x0, sy, x1, sy + 1, color);
        }
    }

    public void drawWrapped(
        GuiGraphicsExtractor graphics,
        @Nullable Font font,
        FormattedText text,
        int x,
        int y,
        int width,
        int color,
        boolean dropShadow
    ) {
        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        float scale = scaleFor(atlas);
        List<String> lines = wrapLines(atlas, text.getString(), width, scale);
        int lineHeight = Minecraft.getInstance().font.lineHeight;
        for (int i = 0; i < lines.size(); i++) {
            this.drawString(graphics, font, lines.get(i), x, y + i * lineHeight, color, dropShadow);
        }
    }

    public void drawCentered(GuiGraphicsExtractor graphics, @Nullable Font font, Component text, int x, int y, int color) {
        drawCentered(graphics, font, text.getVisualOrderText(), x, y, color);
    }

    public void drawCentered(GuiGraphicsExtractor graphics, @Nullable Font font, FormattedCharSequence text, int x, int y, int color) {
        String value = flattenToString(text);
        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        float scale = scaleFor(atlas);
        int drawX = x - Math.round(atlas.measureText(value) * scale) / 2;
        this.drawFormatted(graphics, font, text, drawX, y, color, false);
    }

    private static int colorFromStyle(Style style, int defaultColor) {
        return style.getColor() != null ? style.getColor().getValue() | 0xFF000000 : defaultColor;
    }

    private static List<String> wrapLines(SdfGlyphAtlas atlas, String text, int maxWidth, float scale) {
        return dev.anvilcraft.lib.v2.font.ALFont.wrapLines(atlas, text, maxWidth, scale);
    }

    private static String flattenToString(FormattedCharSequence text) {
        StringBuilder buf = new StringBuilder();
        text.accept((index, style, cp) -> {
            buf.appendCodePoint(cp);
            return true;
        });
        return buf.toString();
    }

    private static void drawAtlasPipeline(
        GuiGraphicsExtractor graphics,
        SdfTextLayout.PageQuads pq,
        GpuSampler diffuseSampler,
        int color
    ) {
        if (pq.atlasTexture() == null || pq.quads().isEmpty()) return;
        SdfTextRenderState state = new SdfTextRenderState(
            graphics.pose(),
            pq.quads(),
            pq.atlasTexture(),
            diffuseSampler,
            pq.pageWidth(),
            pq.pageHeight(),
            color,
            graphics.peekScissorStack()
        );
        graphics.submitGuiElementRenderState(state);
    }
}

