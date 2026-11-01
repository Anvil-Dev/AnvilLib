package dev.anvilcraft.lib.v2.font;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Font module render pipeline registration.
 *
 * <p>The pipeline is ready for SDF text quad rendering and will be consumed by the
 * runtime text render state in a follow-up step.</p>
 */
@ApiStatus.Internal
@Slf4j
@EventBusSubscriber(modid = AnvilLibFont.MOD_ID, value = Dist.CLIENT)
public final class ALFPipelines {
    public static final VertexFormat SDF_TEXT_FORMAT = VertexFormat.builder(0)
        .addAttribute("Position", com.mojang.blaze3d.GpuFormat.RGB32_FLOAT)
        .addAttribute("Color", com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)
        .addAttribute("UV0", com.mojang.blaze3d.GpuFormat.RG32_FLOAT)
        .build();

    public static final RenderPipeline SDF_TEXT = RenderPipeline.builder()
        .withLocation(AnvilLibFont.of("sdf_text"))
        .withVertexShader(AnvilLibFont.of("core/sdf_text"))
        .withFragmentShader(AnvilLibFont.of("core/sdf_text"))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexBinding(0, SDF_TEXT_FORMAT).withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS)
        .withBindGroupLayout(com.mojang.blaze3d.pipeline.BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(com.mojang.blaze3d.pipeline.BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(com.mojang.blaze3d.pipeline.BindGroupLayout.builder().withSampler("DiffuseSampler").build())
        .withCull(false)
        .build();

    private ALFPipelines() {
    }

    @SubscribeEvent
    public static void on(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SDF_TEXT);
        log.info("Registered SDF_TEXT pipeline: {}", SDF_TEXT);
    }
}


