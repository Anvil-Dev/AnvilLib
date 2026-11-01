package dev.anvilcraft.lib.v2.rendering;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import dev.anvilcraft.lib.v2.rendering.bloom.TransformsUbo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

@EventBusSubscriber(Dist.CLIENT)
public class ALRPipelines {
    public static final RenderPipeline.Snippet POST_PASS = RenderPipeline.builder()
        .withVertexShader(AnvilLibRendering.location("core/blit"))
        .withColorTargetState(ColorTargetState.DEFAULT)
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("Transforms", UniformType.UNIFORM_BUFFER).build())
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX).withPrimitiveTopology(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS)
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("DiffuseSampler", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withCull(false)
        .buildSnippet();

    public static final RenderPipeline GLITCH = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("glitch"))
        .withFragmentShader(AnvilLibRendering.location("core/glitch"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("GlitchParameters", UniformType.UNIFORM_BUFFER).build())
        .build();

    public static final RenderPipeline BLUR = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("blur"))
        .withFragmentShader(AnvilLibRendering.location("core/blur"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("BlurParameters", UniformType.UNIFORM_BUFFER).build())
        .build();

    public static final RenderPipeline APPLY_BLOOM = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("apply_bloom"))
        .withFragmentShader(AnvilLibRendering.location("core/apply_bloom"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("GameSampler", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("BloomParameters", UniformType.UNIFORM_BUFFER).build())
        .build();

    public static final RenderPipeline DOWNSAMPLE = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("down_sample"))
        .withFragmentShader(AnvilLibRendering.location("core/down_sample"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("BloomParameters", UniformType.UNIFORM_BUFFER).build())
        .build();

    public static final RenderPipeline UPSAMPLE = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("up_sample"))
        .withFragmentShader(AnvilLibRendering.location("core/up_sample"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("PreviousSampler", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("BloomParameters", UniformType.UNIFORM_BUFFER).build())
        .build();

    public static final VertexFormat SDF_GRAPHICS_FORMAT = VertexFormat.builder(0)
        .addAttribute("Position", GpuFormat.RGB32_FLOAT)
        .addAttribute("Color", GpuFormat.RGBA8_UNORM)
        .addAttribute("UV0", GpuFormat.RG32_FLOAT)
        .addAttribute("UV1", GpuFormat.RG16_SINT)
        .build();

    public static final RenderPipeline SDF_GRAPHICS = RenderPipeline.builder()
        .withLocation(AnvilLibRendering.location("sdf_graphics"))
        .withVertexShader(AnvilLibRendering.location("core/sdf_graphics"))
        .withFragmentShader(AnvilLibRendering.location("core/sdf_graphics"))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexBinding(0, SDF_GRAPHICS_FORMAT).withPrimitiveTopology(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS)
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("SDFParameters", UniformType.UNIFORM_BUFFER).build())
        .withCull(false)
        .build();

    public static final RenderPipeline OCCLUSION_QUERY = RenderPipeline.builder()
        .withLocation(AnvilLibRendering.location("occlusion_query"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION).withPrimitiveTopology(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS)
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("Transforms", UniformType.UNIFORM_BUFFER).build())
        .withVertexShader(AnvilLibRendering.location("core/occlusion_query"))
        .withFragmentShader(AnvilLibRendering.location("core/occlusion_query"))
        .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .withCull(false)
        .build();


    @ApiStatus.Internal
    @SubscribeEvent
    public static void on(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(GLITCH);
        event.registerPipeline(BLUR);
        event.registerPipeline(APPLY_BLOOM);
        event.registerPipeline(DOWNSAMPLE);
        event.registerPipeline(UPSAMPLE);

        event.registerPipeline(SDF_GRAPHICS);

        event.registerPipeline(OCCLUSION_QUERY);
    }
}
