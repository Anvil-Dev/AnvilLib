package dev.anvilcraft.lib.v2.wheel.client.init;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.wheel.AnvilLibWheel;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class LibRenders {
    public static final RenderPipeline.Snippet SNIPPET_COMMON = RenderPipeline.builder()
        .withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .buildSnippet();

    public static final RenderPipeline RING_PIPELINE = RenderPipeline.builder(SNIPPET_COMMON)
        .withLocation(AnvilLibWheel.of("pipeline/ring"))
        .withVertexShader("core/position_color")
        .withFragmentShader(AnvilLibWheel.of("core/ring"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS)
        .withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder().withUniform("RingUniform", UniformType.UNIFORM_BUFFER).build())
        .build();

    public static final RenderPipeline SELECTION_PIPELINE = RenderPipeline.builder(SNIPPET_COMMON)
        .withLocation(AnvilLibWheel.of("pipeline/selection"))
        .withVertexShader("core/position_color")
        .withFragmentShader(AnvilLibWheel.of("core/selection"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS)
        .withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder().withUniform("SelectionUniform", UniformType.UNIFORM_BUFFER).build())
        .build();

    public static final RenderPipeline ANNULAR_SECTOR_PIPELINE = RenderPipeline.builder(SNIPPET_COMMON)
        .withLocation(AnvilLibWheel.of("pipeline/annular_sector"))
        .withVertexShader("core/position_color")
        .withFragmentShader(AnvilLibWheel.of("core/annular_sector"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS)
        .withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder().withUniform("AnnularSectorUniform", UniformType.UNIFORM_BUFFER).build())
        .build();

    public static final RenderPipeline FROSTED_DISC_PIPELINE = RenderPipeline.builder(SNIPPET_COMMON)
        .withLocation(AnvilLibWheel.of("pipeline/frosted_disc"))
        .withVertexShader("core/position_tex_color")
        .withFragmentShader(AnvilLibWheel.of("core/frosted_disc"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS)
        .withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder().withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withBindGroupLayout(com.mojang.renderpearl.api.pipeline.BindGroupLayout.builder().withUniform("FrostedDiscUniform", UniformType.UNIFORM_BUFFER).build())
        .build();
}
