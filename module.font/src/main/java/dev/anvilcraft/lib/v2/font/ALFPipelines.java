package dev.anvilcraft.lib.v2.font;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Font module SDF shader registration.
 *
 * <p>The SDF text shader is registered via {@link RegisterShadersEvent} and can be
 * bound through {@link com.mojang.blaze3d.systems.RenderSystem#setShader} when drawing
 * SDF text quads on the GUI.</p>
 */
@ApiStatus.Internal
@Slf4j
@EventBusSubscriber(modid = AnvilLibFont.MOD_ID, value = Dist.CLIENT)
public final class ALFPipelines {
    /**
     * The element names (Position / Color / UV0) match the vertex inputs declared in {@code sdf_text.vsh}.
     */
    public static final VertexFormat SDF_TEXT_FORMAT = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV", VertexFormatElement.UV)
        .build();

    @Getter
    private static @Nullable ShaderInstance sdfTextShader;

    private ALFPipelines() {
    }

    @SubscribeEvent
    public static void on(RegisterShadersEvent event) {
        try {
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    AnvilLibFont.of("sdf_text"),
                    SDF_TEXT_FORMAT
                ),
                it -> sdfTextShader = it
            );
            log.info("Registered SDF_TEXT shader: {}", AnvilLibFont.of("sdf_text"));
        } catch (IOException e) {
            log.error("Failed to register SDF text shader", e);
        }
    }
}
