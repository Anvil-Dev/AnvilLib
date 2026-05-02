package dev.anvilcraft.lib.v2.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ALRPipelines {
    public static final RenderPipeline.Snippet POST_PASS = RenderPipeline.builder()
        .withVertexShader(ALRendering.location("core/blit"))
        .withUniform("Transforms", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
        .withSampler("DiffuseSampler")
        .withCull(false)
        .buildSnippet();

    public static final RenderPipeline BLUR = RenderPipeline.builder(POST_PASS)
        .withLocation(ALRendering.location("blur"))
        .withFragmentShader(ALRendering.location("core/blur"))
        .withUniform("BlurParameters", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline APPLY_BLOOM = RenderPipeline.builder(POST_PASS)
        .withLocation(ALRendering.location("apply_bloom"))
        .withFragmentShader(ALRendering.location("core/apply_bloom"))
        .withSampler("GameSampler")
        .withUniform("BloomParameters", UniformType.UNIFORM_BUFFER)
        .build();

    public static final RenderPipeline DOWNSAMPLE = RenderPipeline.builder(POST_PASS)
            .withLocation(ALRendering.location("down_sample"))
            .withFragmentShader(ALRendering.location("core/down_sample"))
            .withSampler("DiffuseSampler")
            .withUniform("BloomParameters", UniformType.UNIFORM_BUFFER)
            .build();

    public static final RenderPipeline UPSAMPLE = RenderPipeline.builder(POST_PASS)
            .withLocation(ALRendering.location("up_sample"))
            .withFragmentShader(ALRendering.location("core/up_sample"))
            .withSampler("DiffuseSampler")
            .withSampler("PreviousSampler")
            .withUniform("BloomParameters", UniformType.UNIFORM_BUFFER)
            .build();


    @SubscribeEvent
    public static void on(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(BLUR);
        event.registerPipeline(APPLY_BLOOM);
        event.registerPipeline(DOWNSAMPLE);
        event.registerPipeline(UPSAMPLE);
    }
}
