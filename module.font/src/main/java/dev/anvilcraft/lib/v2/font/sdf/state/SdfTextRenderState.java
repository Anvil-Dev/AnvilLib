package dev.anvilcraft.lib.v2.font.sdf.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.font.ALFPipelines;
import dev.anvilcraft.lib.v2.font.sdf.SdfTextLayout;
import dev.anvilcraft.lib.v2.rendering.state.LibGuiElementRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Render state for SDF text rendering.
 *
 * <p>Coordinates are in screen space and will be transformed via the pose matrix.</p>
 */
public record SdfTextRenderState(
    Matrix3x2f pose,
    List<SdfTextLayout.GlyphQuad> glyphs,
    Identifier atlasTexture,
    GpuSampler diffuseSampler,
    int atlasWidth,
    int atlasHeight,
    int color,
    @Nullable ScreenRectangle scissorArea
) implements LibGuiElementRenderState {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdfTextRenderState.class);

    @Override
    public RenderPipeline pipeline() {
        return ALFPipelines.SDF_TEXT;
    }

    @Override
    public TextureSetup textureSetup() {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(this.atlasTexture);
        return TextureSetup.singleTexture(texture.getTextureView(), this.diffuseSampler);
    }

    @Override
    public void executeDrawAfterSetPipline(RenderPass renderPass) {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(this.atlasTexture);
        renderPass.bindTexture("DiffuseSampler", texture.getTextureView(), diffuseSampler);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        for (SdfTextLayout.GlyphQuad quad : this.glyphs) {
            // Build quad vertices in screen space
            // The pose matrix will handle transformation to clip space

            float x0 = quad.x0();
            float y0 = quad.y0();
            float x1 = quad.x1();
            float y1 = quad.y1();

            float u0 = quad.u0();
            float v0 = quad.v0();
            float u1 = quad.u1();
            float v1 = quad.v1();

            // Draw quad as two triangles (4 vertices in QUADS mode)
            // Vertex 0: top-left
            consumer.addVertexWith2DPose(this.pose, x0, y0)
                .setColor(this.color)
                .setUv(u0, v0);

            // Vertex 1: bottom-left
            consumer.addVertexWith2DPose(this.pose, x0, y1)
                .setColor(this.color)
                .setUv(u0, v1);

            // Vertex 2: bottom-right
            consumer.addVertexWith2DPose(this.pose, x1, y1)
                .setColor(this.color)
                .setUv(u1, v1);

            // Vertex 3: top-right
            consumer.addVertexWith2DPose(this.pose, x1, y0)
                .setColor(this.color)
                .setUv(u1, v0);
        }
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        if (this.glyphs.isEmpty()) {
            return null;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (SdfTextLayout.GlyphQuad quad : this.glyphs) {
            minX = Math.min(minX, quad.x0());
            minY = Math.min(minY, quad.y0());
            maxX = Math.max(maxX, quad.x1());
            maxY = Math.max(maxY, quad.y1());
        }

        if (minX >= maxX || minY >= maxY) {
            return null;
        }

        return LibGuiElementRenderState.getBounds(
            this.pose,
            minX,
            minY,
            maxX,
            maxY,
            this.scissorArea
        );
    }
}


