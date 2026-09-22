package dev.anvilcraft.lib.v2.renderer.projection;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.LinkedHashMap;
import java.util.Map;

final class ProjectionRenderTypes {
    private static final RenderType BLOCK_GHOST = RenderType.create(
        "anvillib_renderer:projection", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, true, true,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(ProjectionShaders::shader))
            .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
            .createCompositeState(true)
    );
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
    }

    static RenderType ghost(RenderType original) {
        if (original == RenderType.solid() || original == RenderType.cutout()
            || original == RenderType.cutoutMipped() || original == RenderType.tripwire() || original == RenderType.translucent()) {
            return BLOCK_GHOST;
        }
        if (TYPES.size() >= 128) TYPES.clear();
        return TYPES.computeIfAbsent(original, ProjectionRenderTypes::translucent);
    }
}
