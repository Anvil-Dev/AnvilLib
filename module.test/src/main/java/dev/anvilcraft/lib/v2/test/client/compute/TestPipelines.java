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
        .withShaderStorage("Input")
        .withShaderStorage("Output")
        .withShader(AnvilLibTest.of("compute/test.csh"))
        .build();

    public static final ALRComputePipeline EMPTY = ALRComputePipeline.builder()
        .withShader(AnvilLibTest.of("compute/empty.csh"))
        .build();

    @SubscribeEvent
    public static void on(RegisterComputePipelinesEvent event) {
        event.registerPipeline(TEST);
    }
}
