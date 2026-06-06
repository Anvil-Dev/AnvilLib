package dev.anvilcraft.lib.v2.test.client.compute;

import dev.anvilcraft.lib.v2.rendering.event.RegisterComputePipelinesEvent;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class TestPipelines {
    public static final ALRComputePipeline TEST = ALRComputePipeline.builder()
        .withName(AnvilLibTest.of("test"))
        .withShaderStorage("Input")
        .withShaderStorage("Output")
        .withShader(AnvilLibTest.of("compute/test.csh"))
        .build();

    public static final ALRComputePipeline EMPTY = ALRComputePipeline.builder()
        .withName(AnvilLibTest.of("empty"))
        .withShader(AnvilLibTest.of("compute/empty.csh"))
        .build();

    public static final ALRComputePipeline ADD = ALRComputePipeline.builder()
        .withName(AnvilLibTest.of("add"))
        .withShaderStorage("Input")
        .withShaderStorage("Output")
        .withUniformBlock("AddParameter")
        .withAtomicCounter("Counter")
        .withShader(AnvilLibTest.of("compute/add.csh"))
        .build();

    public static final ALRComputePipeline BLUR = ALRComputePipeline.builder()
        .withName(AnvilLibTest.of("image_and_sampler"))
        .withUniformBlock("BlurParam")
        .withTexture("InTexture")
        .withWriteOnlyImage("OutImage")
        .withShader(AnvilLibTest.of("compute/image_and_sampler.csh"))
        .build();

    @SubscribeEvent
    public static void on(RegisterComputePipelinesEvent event) {
        event.registerPipeline(TEST);
        event.registerPipeline(EMPTY);
        event.registerPipeline(ADD);
        event.registerPipeline(BLUR);
    }
}
