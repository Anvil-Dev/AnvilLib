package dev.anvilcraft.lib.v2.font.sdf;

import com.google.common.annotations.Beta;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.font.ALFPipelines;
import dev.anvilcraft.lib.v2.font.sdf.state.SdfTextRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.GlStateBackup;
import javax.annotation.Nullable;

import java.awt.Font;
import java.util.List;

/**
 * SDF text renderer that draws strings via the SDF text shader.
 * <p>
 * Uses a CPU-generated SDF glyph atlas uploaded to a GPU texture,
 * sampled by a custom fragment shader for smooth anti-aliased text.
 */
@Beta
public final class SdfTextRenderer {
    public SdfTextRenderer() {
    }

    /**
     * Get the SDF atlas for a font, blocking until it is ready on first access.
     * The atlas is cached forever, so only the very first call for a particular
     * font blocks (typically ~200-400 ms while the glyph atlas is built).
     */
    private static SdfGlyphAtlas getAtlas(@Nullable Font font) {
        return SdfGlyphAtlas.getOrCreate(font).join();
    }

    public void drawString(
        GuiGraphics graphics,
        @Nullable Font font,
        @Nullable String text,
        int x,
        int y,
        int color,
        boolean dropShadow
    ) {
        if (text == null || text.isEmpty()) return;
        this.drawFormatted(graphics, font, FormattedCharSequence.forward(text, Style.EMPTY), x, y, color, dropShadow);
    }

    private static float scaleFor(SdfGlyphAtlas atlas) {
        return Minecraft.getInstance().font.lineHeight / (float) atlas.awtHeight();
    }

    /**
     * Derive a styled font for bold/italic.
     */
    private static Font styledFont(@Nullable Font base, boolean bold, boolean italic) {
        return SdfTextMetrics.styledFont(base, Style.EMPTY.withBold(bold).withItalic(italic));
    }

    /**
     * Replace codepoint with random ASCII for obfuscated style.
     */
    private static int obfuscateCodepoint(int index, long t) {
        int r = (int) (((long) index * 7L + t) % 95L);
        return 32 + r;
    }

    public void drawComponent(
        GuiGraphics graphics,
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
        GuiGraphics graphics,
        @Nullable Font font,
        FormattedCharSequence text,
        int x,
        int y,
        int color,
        boolean dropShadow
    ) {
        long obfuscationTick = System.currentTimeMillis() / 300L;
        if (dropShadow) {
            this.drawFormattedPass(graphics, font, text, x + 1, y + 1, color, true, obfuscationTick);
            // Decorations use the GUI buffer, so complete the whole shadow before the foreground.
            graphics.flush();
        }
        this.drawFormattedPass(graphics, font, text, x, y, color, false, obfuscationTick);
    }

    private void drawFormattedPass(
        GuiGraphics graphics, @Nullable Font font, FormattedCharSequence text,
        int x, int y, int color, boolean shadow, long obfuscationTick
    ) {
        int[] pen = {
            x,
            x
        };
        StringBuilder buf = new StringBuilder();
        int[] segColor = {color};
        boolean[] segBold = {false};
        boolean[] segItalic = {false};
        boolean[] segUnderline = {false};
        boolean[] segStrikethrough = {false};

        text.accept((index, style, codepoint) -> {
            if (style.isObfuscated()) {
                codepoint = obfuscateCodepoint(index, obfuscationTick);
            }

            int c = colorFromStyle(style, color);
            if (shadow) c = shadowColor(c);
            boolean b = style.isBold();
            boolean i = style.isItalic();
            boolean u = style.isUnderlined();
            boolean s = style.isStrikethrough();

            if ((c != segColor[0] || b != segBold[0] || i != segItalic[0]
                || u != segUnderline[0] || s != segStrikethrough[0]) && !buf.isEmpty()) {
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
            segUnderline[0] = u;
            segStrikethrough[0] = s;
            return true;
        });

        if (!buf.isEmpty()) {
            Font segFont = styledFont(font, segBold[0], segItalic[0]);
            pen[0] = flushFormattedSegment(graphics, segFont, buf.toString(), pen[0], y, segColor[0]);
            drawDecorations(graphics, pen[1], pen[0], y, segColor[0], segUnderline[0], segStrikethrough[0]);
        }
    }

    private int flushFormattedSegment(GuiGraphics graphics, @Nullable Font font, String text, int x, int y, int color) {
        SdfGlyphAtlas atlas = getAtlas(font);
        float scale = scaleFor(atlas);
        SdfAtlasTexture.ensureUploaded(atlas);
        SdfTextLayout layout = SdfTextLayout.fromAtlas(atlas, text, x, y, scale);
        for (SdfTextLayout.PageQuads pq : layout.pages()) {
            if (pq.atlasTexture() != null && !pq.quads().isEmpty()) {
                drawAtlas(graphics, pq, color, x, y);
            }
        }
        return x + layout.width();
    }

    /**
     * Draw underline and/or strikethrough lines relative to baseline.
     */
    private static void drawDecorations(
        GuiGraphics graphics,
        int x0,
        int x1,
        int y,
        int color,
        boolean underline,
        boolean strikethrough
    ) {
        if (x1 <= x0) return;
        int lh = Minecraft.getInstance().font.lineHeight;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(1.0F, 0.5F, 1.0F);
        if (strikethrough) {
            int sy = (y + lh / 2) * 2;
            graphics.fill(x0, sy, x1, sy + 1, color);
        }
        if (underline) {
            int sy = (y + lh - 2) * 2;
            graphics.fill(x0, sy, x1, sy + 1, color);
        }
        pose.popPose();
    }

    public void drawWrapped(
        GuiGraphics graphics,
        @Nullable Font font,
        FormattedText text,
        int x,
        int y,
        int width,
        int color,
        boolean dropShadow
    ) {
        int lineHeight = Minecraft.getInstance().font.lineHeight;
        SdfTextMetrics metrics = new SdfTextMetrics(font, lineHeight);
        List<FormattedCharSequence> lines = Language.getInstance().getVisualOrder(metrics.split(text, width));
        for (int i = 0; i < lines.size(); i++) {
            this.drawFormatted(graphics, font, lines.get(i), x, y + i * lineHeight, color, dropShadow);
        }
    }

    public void drawCentered(GuiGraphics graphics, @Nullable Font font, Component text, int x, int y, int color) {
        drawCentered(graphics, font, text.getVisualOrderText(), x, y, color);
    }

    public void drawCentered(GuiGraphics graphics, @Nullable Font font, FormattedCharSequence text, int x, int y, int color) {
        SdfTextMetrics metrics = new SdfTextMetrics(font, Minecraft.getInstance().font.lineHeight);
        int drawX = x - (int) metrics.splitter().stringWidth(text) / 2;
        this.drawFormatted(graphics, font, text, drawX, y, color, false);
    }

    private static int colorFromStyle(Style style, int defaultColor) {
        return style.getColor() != null ? style.getColor().getValue() | (defaultColor & 0xFF000000) : defaultColor;
    }

    private static int shadowColor(int color) {
        return (color & 0xFF000000) | ((color & 0x00FCFCFC) >> 2);
    }

    private static void drawAtlas(
        GuiGraphics graphics,
        SdfTextLayout.PageQuads pq,
        int color,
        int originX,
        int originY
    ) {
        if (pq.atlasTexture() == null || pq.quads().isEmpty()) return;
        SdfTextRenderState state = new SdfTextRenderState(
            new org.joml.Matrix4f(graphics.pose().last().pose()),
            pq.quads(),
            pq.atlasTexture(),
            color,
            originX,
            originY
        );
        GlStateBackup backup = new GlStateBackup();
        RenderSystem.backupGlState(backup);
        var previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        try {
            // Managed GUI backgrounds must reach the GPU before immediate text.
            graphics.flush();
            if (ALFPipelines.getSdfTextShader() == null) return;
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(ALFPipelines::getSdfTextShader);
            RenderSystem.setShaderTexture(0, state.atlasTexture());
            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, ALFPipelines.SDF_TEXT_FORMAT);
            state.buildVertices(builder);
            BufferUploader.drawWithShader(builder.buildOrThrow());
        } finally {
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.restoreGlState(backup);
        }
    }
}
