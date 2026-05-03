package dev.anvilcraft.lib.v2.wheel.client.init;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.wheel.AnvilLibWheel;

public class LibRenders {
    public static final RenderPipeline.Snippet SNIPPET_COMMON = RenderPipeline.builder()
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .buildSnippet();

    public static final RenderPipeline RING_PIPELINE = RenderPipeline.builder(SNIPPET_COMMON)
        .withLocation(AnvilLibWheel.of("pipeline/ring"))
        .withVertexShader("core/position_color")
        .withFragmentShader(AnvilLibWheel.of("core/ring"))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .withUniform("RingUniform", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline SELECTION_PIPELINE = RenderPipeline.builder(SNIPPET_COMMON)
        .withLocation(AnvilLibWheel.of("pipeline/selection"))
        .withVertexShader("core/position_color")
        .withFragmentShader(AnvilLibWheel.of("core/selection"))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .withUniform("SelectionUniform", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline ANNULAR_SECTOR_PIPELINE = RenderPipeline.builder(SNIPPET_COMMON)
        .withLocation(AnvilLibWheel.of("pipeline/annular_sector"))
        .withVertexShader("core/position_color")
        .withFragmentShader(AnvilLibWheel.of("core/annular_sector"))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .withUniform("AnnularSectorUniform", UniformType.UNIFORM_BUFFER)
        .build();
}
