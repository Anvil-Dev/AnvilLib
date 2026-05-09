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
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SDF text renderer that draws strings via the SDF text render pipeline.
 * <p>
 * Uses a CPU-generated SDF glyph atlas uploaded to a GPU texture,
 * sampled by a custom fragment shader for smooth anti-aliased text.
 */
public final class SdfTextRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdfTextRenderer.class);

    private final GpuSampler diffuseSampler = RenderSystem.getSamplerCache()
        .getClampToEdge(FilterMode.LINEAR);

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
        if (text == null || text.isEmpty()) {
            return;
        }

        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        float scale = scaleFor(atlas);
        SdfTextLayout layout = SdfTextLayout.fromAtlas(atlas, text, x, y, scale);
        if (layout.quads().isEmpty()) {
            LOGGER.warn("SDF drawString: empty layout for text='{}' font={}", text, font);
            return;
        }

        Identifier atlasTexture = SdfAtlasTexture.getOrUpload(atlas);
        drawAtlasPipeline(
            graphics,
            layout,
            atlasTexture,
            this.diffuseSampler,
            atlas.atlasImage().getWidth(),
            atlas.atlasImage().getHeight(),
            color
        );
    }

    private static float scaleFor(SdfGlyphAtlas atlas) {
        return Minecraft.getInstance().font.lineHeight / (float) atlas.awtHeight();
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
        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        float scale = scaleFor(atlas);
        Identifier atlasTexture = SdfAtlasTexture.getOrUpload(atlas);
        int atlasW = atlas.atlasImage().getWidth();
        int atlasH = atlas.atlasImage().getHeight();

        int[] pen = {x};
        StringBuilder buf = new StringBuilder();
        int[] segColor = {color};

        text.accept((index, style, codepoint) -> {
            int c = colorFromStyle(style, color);
            if (c != segColor[0] && !buf.isEmpty()) {
                pen[0] = flushSegment(graphics, atlas, atlasTexture, atlasW, atlasH, scale,
                    buf.toString(), pen[0], y, segColor[0]);
                buf.setLength(0);
            }
            buf.appendCodePoint(codepoint);
            segColor[0] = c;
            return true;
        });

        if (!buf.isEmpty()) {
            flushSegment(graphics, atlas, atlasTexture, atlasW, atlasH, scale,
                buf.toString(), pen[0], y, segColor[0]);
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
        String value = text.getString();
        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        float scale = scaleFor(atlas);
        int drawX = x - Math.round(atlas.measureText(value) * scale) / 2;
        this.drawString(graphics, font, value, drawX, y, color, false);
    }

    public void drawCentered(GuiGraphicsExtractor graphics, @Nullable Font font, FormattedCharSequence text, int x, int y, int color) {
        String value = flattenToString(text);
        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        float scale = scaleFor(atlas);
        int drawX = x - Math.round(atlas.measureText(value) * scale) / 2;
        this.drawString(graphics, font, value, drawX, y, color, false);
    }

    private int flushSegment(
        GuiGraphicsExtractor graphics,
        SdfGlyphAtlas atlas,
        Identifier atlasTexture,
        int atlasW,
        int atlasH,
        float scale,
        String text,
        int x,
        int y,
        int color
    ) {
        SdfTextLayout layout = SdfTextLayout.fromAtlas(atlas, text, x, y, scale);
        if (!layout.quads().isEmpty()) {
            drawAtlasPipeline(graphics, layout, atlasTexture, this.diffuseSampler, atlasW, atlasH, color);
        }
        return x + layout.width();
    }

    private static int colorFromStyle(Style style, int defaultColor) {
        return style.getColor() != null ? style.getColor().getValue() : defaultColor;
    }

    private static List<String> wrapLines(SdfGlyphAtlas atlas, String text, int maxWidth, float scale) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }

        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (Math.round(atlas.measureText(candidate) * scale) > maxWidth) {
                    if (line.isEmpty()) {
                        lines.add(word);
                    } else {
                        lines.add(line.toString());
                        line = new StringBuilder(word);
                    }
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
        }

        return lines;
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
        SdfTextLayout layout,
        Identifier atlasTexture,
        GpuSampler diffuseSampler,
        int atlasWidth,
        int atlasHeight,
        int color
    ) {
        SdfTextRenderState state = new SdfTextRenderState(
            graphics.pose(),
            layout.quads(),
            atlasTexture,
            diffuseSampler,
            atlasWidth,
            atlasHeight,
            color,
            graphics.peekScissorStack()
        );

        graphics.submitGuiElementRenderState(state);
    }
}

