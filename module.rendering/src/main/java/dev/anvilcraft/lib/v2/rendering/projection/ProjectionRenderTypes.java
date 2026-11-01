package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ProjectionRenderTypes {
    static final RenderPipeline BLOCK_PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .withCull(true)
        .withFragmentShader(AnvilLibRendering.location("core/structure_projection"))
        .withLocation(AnvilLibRendering.location("pipeline/structure_projection")).build();
    static final RenderType BLOCK_GHOST = RenderType.create("anvillib_rendering:structure_projection",
        RenderSetup.builder(BLOCK_PIPELINE).useLightmap().sortOnUpload()
            .withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet()).createRenderSetup());
    static final RenderPipeline BLOCK_DEPTH_PIPELINE = BLOCK_PIPELINE.toBuilder()
        .withLocation(AnvilLibRendering.location("pipeline/structure_projection_depth"))
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withColorTargetState(new ColorTargetState(Optional.empty(), com.mojang.blaze3d.GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE)).build();
    static final RenderType BLOCK_DEPTH = RenderType.create("anvillib_rendering:structure_projection_depth",
        RenderSetup.builder(BLOCK_DEPTH_PIPELINE).useLightmap()
            .withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet()).createRenderSetup());
    static final RenderType OVERLAY = RenderType.create("anvillib_rendering:projection_overlay",
        RenderSetup.builder(RenderPipelines.DEBUG_QUADS).createRenderSetup());
    private static final Map<RenderType, RenderType> TYPES = new LinkedHashMap<>();
    private static final Map<RenderType, RenderType> DEPTH_TYPES = new LinkedHashMap<>();
    private static int serial;

    private ProjectionRenderTypes() {
    }

    /** Block/fluid layer without entity diffuse lighting, which is already baked into block quads. */
    public static RenderType blocks() {
        return BLOCK_GHOST;
    }

    static void clear() {
        TYPES.clear();
        DEPTH_TYPES.clear();
    }

    static RenderType ghost(RenderType original, boolean depthOnly) {
        if (original.format() == DefaultVertexFormat.BLOCK) return depthOnly ? BLOCK_DEPTH : BLOCK_GHOST;
        var types = depthOnly ? DEPTH_TYPES : TYPES;
        if (types.size() >= 128) types.clear();
        return types.computeIfAbsent(original, type -> {
            var previous = type.state;
            boolean entity = type.format() == RenderPipelines.ENTITY_TRANSLUCENT_CULL.getVertexFormatBinding(0)
                && previous.textures.containsKey("Sampler0");
            var pipeline = (entity && !type.hasBlending() ? RenderPipelines.ENTITY_TRANSLUCENT_CULL : type.pipeline()).toBuilder()
                .withLocation(AnvilLibRendering.location("pipeline/projection_entity_" + serial++))
                .withCull(type.pipeline().isCull())
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, depthOnly))
                .withColorTargetState(depthOnly ? new ColorTargetState(Optional.empty(), com.mojang.blaze3d.GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE)
                    : new ColorTargetState(BlendFunction.TRANSLUCENT)).build();
            var builder = RenderSetup.builder(pipeline).sortOnUpload()
                .setLayeringTransform(previous.layeringTransform).setOutputTarget(previous.outputTarget)
                .setTextureTransform(previous.textureTransform);
            previous.textures.forEach((name, binding) -> {
                if (com.mojang.blaze3d.pipeline.BindGroupLayout.flattenSamplers(pipeline.getBindGroupLayouts()).contains(name)) builder.withTexture(name, binding.location(), binding.sampler());
            });
            var textures = type.prepare().textures().stream().map(net.minecraft.client.renderer.rendertype.PreparedRenderType.Texture::name).toList();
            if (entity || textures.contains("Sampler1")) builder.useOverlay();
            if (entity || textures.contains("Sampler2")) builder.useLightmap();
            return RenderType.create("anvillib_rendering:projection_entity_" + serial, builder.createRenderSetup());
        });
    }
}
