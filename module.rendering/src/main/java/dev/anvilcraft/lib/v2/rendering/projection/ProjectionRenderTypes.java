package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ProjectionRenderTypes {
    static final RenderPipeline BLOCK_PIPELINE = copy(RenderPipelines.SOLID)
        .withLocation(AnvilLibRendering.location("pipeline/structure_projection"))
        .withFragmentShader(AnvilLibRendering.location("core/structure_projection"))
        .withBlend(BlendFunction.TRANSLUCENT).withDepthWrite(false).withCull(true).build();
    static final RenderPipeline BLOCK_DEPTH_PIPELINE = copy(BLOCK_PIPELINE)
        .withLocation(AnvilLibRendering.location("pipeline/structure_projection_depth"))
        .withoutBlend().withDepthWrite(true).withColorWrite(false).build();
    private static final RenderType.CompositeState BLOCK_STATE = RenderType.CompositeState.builder()
        .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED).setLightmapState(RenderStateShard.LIGHTMAP)
        .createCompositeState(false);
    static final RenderType BLOCK_GHOST = RenderType.create("anvillib_rendering:structure_projection", 786432,
        false, true, BLOCK_PIPELINE, BLOCK_STATE);
    static final RenderType BLOCK_DEPTH = RenderType.create("anvillib_rendering:structure_projection_depth", 786432,
        false, false, BLOCK_DEPTH_PIPELINE, BLOCK_STATE);
    static final RenderType OVERLAY = RenderType.debugQuads();
    private static final Map<RenderType, RenderType> TYPES = new LinkedHashMap<>();
    private static final Map<RenderType, RenderType> DEPTH_TYPES = new LinkedHashMap<>();
    private static int serial;

    private ProjectionRenderTypes() {
    }

    public static RenderType blocks() { return BLOCK_GHOST; }

    static void clear() {
        TYPES.clear();
        DEPTH_TYPES.clear();
    }

    static RenderPipeline pipeline(RenderType type) {
        return ((RenderType.CompositeRenderType) type).renderPipeline;
    }

    static boolean culls(RenderType type) {
        return type instanceof RenderType.CompositeRenderType composite && composite.renderPipeline.isCull();
    }

    static RenderType ghost(RenderType original, boolean depthOnly) {
        if (original.format() == DefaultVertexFormat.BLOCK) return depthOnly ? BLOCK_DEPTH : BLOCK_GHOST;
        if (!(original instanceof RenderType.CompositeRenderType composite)) return original;
        var types = depthOnly ? DEPTH_TYPES : TYPES;
        if (types.size() >= 128) types.clear();
        return types.computeIfAbsent(original, type -> {
            var previous = composite.renderPipeline;
            boolean entity = type.format() == DefaultVertexFormat.NEW_ENTITY;
            var pipeline = copy(entity && previous.getBlendFunction().isEmpty() ? RenderPipelines.ENTITY_TRANSLUCENT : previous)
                .withLocation(AnvilLibRendering.location("pipeline/projection_entity_" + serial++))
                .withCull(previous.isCull()).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(depthOnly).withBlend(BlendFunction.TRANSLUCENT).withColorWrite(!depthOnly).build();
            return RenderType.create("anvillib_rendering:projection_entity_" + serial, type.bufferSize(),
                false, true, pipeline, composite.state);
        });
    }
    /** 旧版管线没有 toBuilder，保留着色器定义、采样器与统一变量后再覆盖投影状态。 */
    private static RenderPipeline.Builder copy(RenderPipeline pipeline) {
        return RenderPipeline.builder(new RenderPipeline.Snippet(
            Optional.of(pipeline.getVertexShader()), Optional.of(pipeline.getFragmentShader()),
            Optional.of(pipeline.getShaderDefines()), Optional.of(pipeline.getSamplers()),
            Optional.of(pipeline.getUniforms()), pipeline.getBlendFunction(),
            Optional.of(pipeline.getDepthTestFunction()), Optional.of(pipeline.getPolygonMode()),
            Optional.of(pipeline.isCull()), Optional.of(pipeline.isWriteColor()),
            Optional.of(pipeline.isWriteAlpha()), Optional.of(pipeline.isWriteDepth()),
            Optional.of(pipeline.getColorLogic()), Optional.of(pipeline.getVertexFormat()),
            Optional.of(pipeline.getVertexFormatMode())))
            .withDepthBias(pipeline.getDepthBiasScaleFactor(), pipeline.getDepthBiasConstant());
    }
}
