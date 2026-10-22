package dev.anvilcraft.lib.v2.renderer.projection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;

import java.util.LinkedHashMap;
import java.util.Map;

final class ProjectionRenderTypes {
    static final RenderType BLOCK_GHOST = RenderType.create(
        "anvillib_renderer:projection", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, true, true,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(ProjectionShaders::shader))
            .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setOutputState(RenderStateShard.MAIN_TARGET)
            .setCullState(RenderStateShard.CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(true)
    );
    static final RenderType OVERLAY = RenderType.debugQuads();
    private static final Map<RenderType, RenderType> DEPTH_TYPES = new LinkedHashMap<>();
    private static final Map<RenderType, RenderType> TYPES = new LinkedHashMap<>();

    private ProjectionRenderTypes() {
    }

    private static RenderType translucent(RenderType original) {
        if (original instanceof RenderType.CompositeRenderType composite
            && composite.state().transparencyState == RenderStateShard.NO_TRANSPARENCY
            && composite.state().textureState instanceof RenderStateShard.TextureStateShard texture) {
            return texture.texture.map(RenderType::entityTranslucentCull).orElse(original);
        }
        return original;
    }

    static void clear() {
        TYPES.clear();
        DEPTH_TYPES.clear();
    }

    static RenderType ghost(RenderType original, boolean depthOnly) {
        if (original.format() == DefaultVertexFormat.BLOCK) original = BLOCK_GHOST;
        var types = depthOnly ? DEPTH_TYPES : TYPES;
        if (types.size() >= 128) types.clear();
        return types.computeIfAbsent(original, type -> new ProjectionPass(translucent(type), depthOnly));
    }

    private static final class ProjectionPass extends RenderType {
        private ProjectionPass(RenderType original, boolean depthOnly) {
            super("anvillib_renderer:projection_" + (depthOnly ? "depth_" : "color_") + original,
                original.format(), original.mode(), original.bufferSize(), false, false, () -> {
                    original.setupRenderState();
                    RenderStateShard.MAIN_TARGET.setupRenderState();
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.depthMask(depthOnly);
                    RenderSystem.colorMask(!depthOnly, !depthOnly, !depthOnly, !depthOnly);
                }, () -> {
                    original.clearRenderState();
                    RenderSystem.colorMask(true, true, true, true);
                    RenderSystem.depthMask(true);
                });
        }
    }
}
