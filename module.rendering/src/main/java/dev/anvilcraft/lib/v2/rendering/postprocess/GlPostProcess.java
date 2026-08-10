package dev.anvilcraft.lib.v2.rendering.postprocess;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * Shared helpers for the 1.21.1 port of the post-processing pipeline.
 * <p>
 * Replaces the 26.1 {@code GpuDevice}/{@code CommandEncoder}/{@code RenderPass} execution layer with the
 * classic {@code RenderTarget} + {@code ShaderInstance} + fullscreen-quad ({@code BufferUploader}/{@code Tesselator})
 * path. The std140 uniform blocks declared by the GLSL are fed through {@link GlUniformBuffer}.
 */
@ApiStatus.Internal
public final class GlPostProcess {
    private GlPostProcess() {
    }

    /**
     * Binds {@code target} as the current framebuffer, sets the viewport and clears its color attachment.
     */
    public static void beginTarget(RenderTarget target) {
        target.bindWrite(true);
        GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager._clear(16384, false);
    }

    /**
     * Applies the given shader (binding its samplers), runs {@code setup} (e.g. UBO binding),
     * draws the fullscreen quad and clears the shader state again.
     */
    public static void draw(ShaderInstance shader, MeshData quad, Consumer<ShaderInstance> setup) {
        shader.apply();
        setup.accept(shader);
        BufferUploader.draw(quad);
        shader.clear();
    }

    /**
     * Builds a fullscreen quad mesh covering {@code [0,width]x[0,height]} in {@code POSITION_TEX} format.
     * The {@code flipV} parameter controls the vertical texture coordinate orientation.
     */
    public static MeshData buildQuad(int width, int height, boolean flipV) {
        Tesselator tesselator = Tesselator.getInstance();
        var builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        if (flipV) {
            builder.addVertex(0, 0, 100).setUv(0, 1);
            builder.addVertex(0, height, 100).setUv(0, 0);
            builder.addVertex(width, height, 100).setUv(1, 0);
            builder.addVertex(width, 0, 100).setUv(1, 1);
        } else {
            builder.addVertex(0, 0, 100).setUv(0, 0);
            builder.addVertex(0, height, 100).setUv(0, 1);
            builder.addVertex(width, height, 100).setUv(1, 1);
            builder.addVertex(width, 0, 100).setUv(1, 0);
        }
        return builder.buildOrThrow();
    }
}
