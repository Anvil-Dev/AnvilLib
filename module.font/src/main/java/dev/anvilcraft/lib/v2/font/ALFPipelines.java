package dev.anvilcraft.lib.v2.font;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Font module render pipeline registration.
 *
 * <p>The pipeline is ready for SDF text quad rendering and will be consumed by the
 * runtime text render state in a follow-up step.</p>
 */
@EventBusSubscriber(modid = AnvilLibFont.MOD_ID, value = Dist.CLIENT)
public final class ALFPipelines {
    private static final Logger LOGGER = LoggerFactory.getLogger(ALFPipelines.class);

    public static final VertexFormat SDF_TEXT_FORMAT = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV", VertexFormatElement.UV)
        .build();

    public static final RenderPipeline SDF_TEXT = RenderPipeline.builder()
        .withLocation(AnvilLibFont.of("sdf_text"))
        .withVertexShader(AnvilLibFont.of("core/sdf_text"))
        .withFragmentShader(AnvilLibFont.of("core/sdf_text"))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexFormat(SDF_TEXT_FORMAT, VertexFormat.Mode.QUADS)
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withSampler("DiffuseSampler")
        .withCull(false)
        .build();

    private ALFPipelines() {
    }

    @SubscribeEvent
    public static void on(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SDF_TEXT);
        LOGGER.info("Registered SDF_TEXT pipeline: {}", SDF_TEXT);
    }
}


