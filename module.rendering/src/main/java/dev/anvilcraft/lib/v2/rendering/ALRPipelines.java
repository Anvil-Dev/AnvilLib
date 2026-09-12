package dev.anvilcraft.lib.v2.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.jetbrains.annotations.ApiStatus;

@EventBusSubscriber(Dist.CLIENT)
public class ALRPipelines {
    public static final RenderPipeline.Snippet POST_PASS = RenderPipeline.builder()
        .withVertexShader(AnvilLibRendering.location("core/blit"))
        .withUniform("Transforms", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
        .withSampler("DiffuseSampler")
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .buildSnippet();


    public static final RenderPipeline BLUR = RenderPipeline.builder(POST_PASS)
        .withLocation(AnvilLibRendering.location("blur"))
        .withFragmentShader(AnvilLibRendering.location("core/blur"))
        .withUniform("BlurParameters", UniformType.UNIFORM_BUFFER)
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
        .withBlend(BlendFunction.TRANSLUCENT)
        .withVertexFormat(SDF_GRAPHICS_FORMAT, VertexFormat.Mode.QUADS)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("SDFParameters", UniformType.UNIFORM_BUFFER)
        .withCull(false)
        .build();


    @ApiStatus.Internal
    @SubscribeEvent
    public static void on(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(BLUR);

        event.registerPipeline(SDF_GRAPHICS);
    }
}
