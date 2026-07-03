package dev.anvilcraft.lib.v2.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ALRPipelines {
    public static final RenderPipeline.Snippet POST_PASS = RenderPipeline.builder()
        .withVertexShader(AnvilLibRendering.location("core/blit"))
        .withUniform("Transforms", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
        .withSampler("DiffuseSampler")
        .withCull(false)
        .buildSnippet();

    public static final RenderPipeline GLITCH = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("glitch"))
        .withFragmentShader(AnvilLibRendering.location("core/glitch"))
        .withUniform("GlitchParameters", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline BLUR = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("blur"))
        .withFragmentShader(AnvilLibRendering.location("core/blur"))
        .withUniform("BlurParameters", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline APPLY_BLOOM = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("apply_bloom"))
        .withFragmentShader(AnvilLibRendering.location("core/apply_bloom"))
        .withSampler("GameSampler")
        .withUniform("BloomParameters", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline DOWNSAMPLE = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("down_sample"))
        .withFragmentShader(AnvilLibRendering.location("core/down_sample"))
        .withSampler("DiffuseSampler")
        .withUniform("BloomParameters", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline UPSAMPLE = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("up_sample"))
        .withFragmentShader(AnvilLibRendering.location("core/up_sample"))
        .withSampler("DiffuseSampler")
        .withSampler("PreviousSampler")
        .withUniform("BloomParameters", UniformType.UNIFORM_BUFFER)
        .build();

    public static final VertexFormat SDF_GRAPHICS_FORMAT = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV)
        .add("UV1", VertexFormatElement.UV1)
        .build();

    public static final RenderPipeline SDF_GRAPHICS = RenderPipeline.builder()
        .withLocation(AnvilLibRendering.location("sdf_graphics"))
        .withVertexShader(AnvilLibRendering.location("core/sdf_graphics"))
        .withFragmentShader(AnvilLibRendering.location("core/sdf_graphics"))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(SDF_GRAPHICS_FORMAT, VertexFormat.Mode.QUADS)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("SDFParameters", UniformType.UNIFORM_BUFFER)
        .withCull(false)
        .build();


    @SubscribeEvent
    public static void on(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(BLUR);
        event.registerPipeline(APPLY_BLOOM);
        event.registerPipeline(DOWNSAMPLE);
        event.registerPipeline(UPSAMPLE);

        event.registerPipeline(SDF_GRAPHICS);
    }
}
