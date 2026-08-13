package dev.anvilcraft.lib.v2.font.sdf.state;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.font.sdf.SdfTextLayout;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Render state for SDF text rendering.
 *
 * <p>Coordinates are in screen space and are transformed via the pose matrix
 * before being submitted to the GPU with the SDF shader.</p>
 */
@ApiStatus.Internal
public record SdfTextRenderState(
    Matrix4f pose,
    List<SdfTextLayout.GlyphQuad> glyphs,
    ResourceLocation atlasTexture,
    int color,
    int originX,
    int originY
) {
    /**
     * Build quad vertices in screen space. The pose matrix transforms them to
     * the GUI coordinate space; the SDF shader then applies the GUI projection.
     */
    public void buildVertices(VertexConsumer consumer) {
        int ox = this.originX;
        int oy = this.originY;
        for (SdfTextLayout.GlyphQuad quad : this.glyphs) {
            float x0 = quad.x0() + ox;
            float y0 = quad.y0() + oy;
            float x1 = quad.x1() + ox;
            float y1 = quad.y1() + oy;

            float u0 = quad.u0();
            float v0 = quad.v0();
            float u1 = quad.u1();
            float v1 = quad.v1();

            // Draw quad as two triangles (4 vertices in QUADS mode)
            // Vertex 0: top-left
            consumer.addVertex(this.pose, x0, y0, 0.0F)
                .setColor(this.color)
                .setUv(u0, v0);

            // Vertex 1: bottom-left
            consumer.addVertex(this.pose, x0, y1, 0.0F)
                .setColor(this.color)
                .setUv(u0, v1);

            // Vertex 2: bottom-right
            consumer.addVertex(this.pose, x1, y1, 0.0F)
                .setColor(this.color)
                .setUv(u1, v1);

            // Vertex 3: top-right
            consumer.addVertex(this.pose, x1, y0, 0.0F)
                .setColor(this.color)
                .setUv(u1, v0);
        }
    }
}
