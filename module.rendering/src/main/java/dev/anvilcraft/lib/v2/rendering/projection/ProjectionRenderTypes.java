package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProjectionRenderTypes {
    static final RenderPipeline BLOCK_PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withFragmentShader(AnvilLibRendering.location("core/structure_projection"))
        .withLocation(AnvilLibRendering.location("pipeline/structure_projection")).build();
    static final RenderType BLOCK_GHOST = RenderType.create("anvillib_rendering:structure_projection",
        RenderSetup.builder(BLOCK_PIPELINE).useLightmap().sortOnUpload()
            .bufferSize(786432).withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet()).createRenderSetup());
    private static final Map<RenderType, RenderType> TYPES = new LinkedHashMap<>();
    private static int serial;

    private ProjectionRenderTypes() {
    }

    /** Block/fluid layer without entity diffuse lighting, which is already baked into block quads. */
    public static RenderType blocks() {
        return BLOCK_GHOST;
    }

    static void clear() {
        TYPES.clear();
    }

    static RenderType ghost(RenderType original) {
        if (original.format() == DefaultVertexFormat.BLOCK) return BLOCK_GHOST;
        if (original.hasBlending()) return original;
        if (TYPES.size() >= 128) TYPES.clear();
        return TYPES.computeIfAbsent(original, type -> {
            var previous = type.state;
            boolean entity = type.format() == RenderPipelines.ENTITY_TRANSLUCENT_CULL.getVertexFormat()
                && previous.textures.containsKey("Sampler0");
            var pipeline = (entity ? RenderPipelines.ENTITY_TRANSLUCENT_CULL : type.pipeline()).toBuilder()
                .withLocation(AnvilLibRendering.location("pipeline/projection_entity_" + serial++))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build();
            var builder = RenderSetup.builder(pipeline).sortOnUpload().bufferSize(type.bufferSize())
                .setLayeringTransform(previous.layeringTransform).setOutputTarget(previous.outputTarget)
                .setTextureTransform(previous.textureTransform);
            previous.textures.forEach((name, binding) -> {
                if (pipeline.getSamplers().contains(name)) builder.withTexture(name, binding.location(), binding.sampler());
            });
            var textures = previous.getTextures();
            if (entity || textures.containsKey("Sampler1")) builder.useOverlay();
            if (entity || textures.containsKey("Sampler2")) builder.useLightmap();
            return RenderType.create("anvillib_rendering:projection_entity_" + serial, builder.createRenderSetup());
        });
    }
}
