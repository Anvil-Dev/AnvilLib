package dev.anvilcraft.lib.v2.font.sdf;

import dev.anvilcraft.lib.v2.font.ALFPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.awt.Font;

/**
 * MVP text renderer facade for the future SDF pipeline.
 *
 * <p>At this stage it warms the glyph atlas cache and renders via vanilla text as a safe fallback.
 * Once the font shader pipeline lands, draw methods can switch to atlas sampling without API changes.</p>
 */
public final class SdfTextRenderer {
    private static final Identifier ASCII_FONT_TEXTURE = Identifier.withDefaultNamespace("textures/font/ascii.png");
    private static final int ASCII_GRID_SIZE = 16;
    private static final int ASCII_GLYPH_SIZE = 8;
    private static final int ASCII_TEXTURE_SIZE = 128;

    private SdfTextRenderer() {
    }

    public static void drawString(
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

        // Prepare cache and geometry; render backend switch can consume this layout directly.
        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        SdfTextLayout layout = SdfTextLayout.fromAtlas(atlas, text, x, y);
        if (layout.quads().isEmpty()) {
            return;
        }

        Identifier atlasTexture = SdfAtlasTexture.getOrUpload(atlas);
        if (!drawAtlasPipeline(graphics, layout, atlasTexture, atlas.atlasImage().getWidth(), atlas.atlasImage().getHeight(), color)) {
            graphics.text(Minecraft.getInstance().font, text, x, y, color, dropShadow);
        }
    }

    public static void drawComponent(
        GuiGraphicsExtractor graphics,
        @Nullable Font font,
        Component text,
        int x,
        int y,
        int color,
        boolean dropShadow
    ) {
        drawString(graphics, font, text.getString(), x, y, color, dropShadow);
    }

    public static void drawFormatted(
        GuiGraphicsExtractor graphics,
        @Nullable Font font,
        FormattedCharSequence text,
        int x,
        int y,
        int color,
        boolean dropShadow
    ) {
        drawString(graphics, font, flatten(text), x, y, color, dropShadow);
    }

    public static void drawWrapped(
        GuiGraphicsExtractor graphics,
        @Nullable Font font,
        FormattedText text,
        int x,
        int y,
        int width,
        int color,
        boolean dropShadow
    ) {
        SdfGlyphAtlas.getOrCreate(font);
        graphics.textWithWordWrap(Minecraft.getInstance().font, text, x, y, width, color, dropShadow);
    }

    public static void drawCentered(
        GuiGraphicsExtractor graphics,
        @Nullable Font font,
        Component text,
        int x,
        int y,
        int color
    ) {
        SdfGlyphAtlas.getOrCreate(font);
        graphics.centeredText(Minecraft.getInstance().font, text, x, y, color);
    }

    public static void drawCentered(
        GuiGraphicsExtractor graphics,
        @Nullable Font font,
        FormattedCharSequence text,
        int x,
        int y,
        int color
    ) {
        String value = flatten(text);
        SdfGlyphAtlas atlas = SdfGlyphAtlas.getOrCreate(font);
        int drawX = x - atlas.measureText(value) / 2;
        drawString(graphics, font, value, drawX, y, color, false);
    }

    private static String flatten(FormattedCharSequence text) {
        StringBuilder builder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    private static boolean drawAsciiPipeline(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        SdfTextLayout layout = SdfTextLayout.fromAsciiTexture(text, x, y, ASCII_GLYPH_SIZE, ASCII_GRID_SIZE, ASCII_TEXTURE_SIZE);
        if (layout.quads().isEmpty()) {
            return false;
        }

        for (SdfTextLayout.GlyphQuad quad : layout.quads()) {
            int u = Math.round(quad.u0() * ASCII_TEXTURE_SIZE);
            int v = Math.round(quad.v0() * ASCII_TEXTURE_SIZE);

            graphics.blit(
                ALFPipelines.SDF_TEXT,
                ASCII_FONT_TEXTURE,
                quad.x0(),
                quad.y0(),
                u,
                v,
                quad.x1() - quad.x0(),
                quad.y1() - quad.y0(),
                ASCII_TEXTURE_SIZE,
                ASCII_TEXTURE_SIZE,
                color
            );
        }

        return true;
    }

    private static boolean drawAtlasPipeline(
        GuiGraphicsExtractor graphics,
        SdfTextLayout layout,
        Identifier atlasTexture,
        int atlasWidth,
        int atlasHeight,
        int color
    ) {
        if (layout.quads().isEmpty()) {
            return false;
        }

        for (SdfTextLayout.GlyphQuad quad : layout.quads()) {
            int u = Math.round(quad.u0() * atlasWidth);
            int v = Math.round(quad.v0() * atlasHeight);
            int w = quad.x1() - quad.x0();
            int h = quad.y1() - quad.y0();

            graphics.blit(
                ALFPipelines.SDF_TEXT,
                atlasTexture,
                quad.x0(),
                quad.y0(),
                u,
                v,
                w,
                h,
                atlasWidth,
                atlasHeight,
                color
            );
        }

        return true;
    }
}

