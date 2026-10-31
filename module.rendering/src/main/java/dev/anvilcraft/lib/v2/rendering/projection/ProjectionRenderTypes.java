package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
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
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withDepthWrite(false)
        .withCull(true)
        .withFragmentShader(AnvilLibRendering.location("core/structure_projection"))
        .withLocation(AnvilLibRendering.location("pipeline/structure_projection")).build();
    static final RenderType BLOCK_GHOST = RenderType.create("anvillib_rendering:structure_projection",
        RenderSetup.builder(BLOCK_PIPELINE).useLightmap().sortOnUpload()
            .bufferSize(786432).withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet()).createRenderSetup());
    static final RenderPipeline BLOCK_DEPTH_PIPELINE = copy(BLOCK_PIPELINE)
        .withLocation(AnvilLibRendering.location("pipeline/structure_projection_depth"))
        .withDepthWrite(true)
        .withoutBlend().withColorWrite(false).build();
    static final RenderType BLOCK_DEPTH = RenderType.create("anvillib_rendering:structure_projection_depth",
        RenderSetup.builder(BLOCK_DEPTH_PIPELINE).useLightmap()
            .bufferSize(786432).withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet()).createRenderSetup());
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
            boolean entity = type.format() == RenderPipelines.ENTITY_TRANSLUCENT.getVertexFormat()
                && previous.textures.containsKey("Sampler0");
            var pipeline = copy(entity && type.pipeline().getBlendFunction().isEmpty() ? RenderPipelines.ENTITY_TRANSLUCENT : type.pipeline())
                .withLocation(AnvilLibRendering.location("pipeline/projection_entity_" + serial++))
                .withCull(type.pipeline().isCull())
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withDepthWrite(depthOnly)
                .withBlend(BlendFunction.TRANSLUCENT).withColorWrite(!depthOnly).build();
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
