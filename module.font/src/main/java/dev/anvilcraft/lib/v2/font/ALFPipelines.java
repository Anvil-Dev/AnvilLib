package dev.anvilcraft.lib.v2.font;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.ApiStatus;
import javax.annotation.Nullable;

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

    private static final net.minecraft.client.renderer.ShaderProgram SDF_TEXT = new net.minecraft.client.renderer.ShaderProgram(
        AnvilLibFont.of("core/sdf_text"), SDF_TEXT_FORMAT, net.minecraft.client.renderer.ShaderDefines.EMPTY);

    public static net.minecraft.client.renderer.ShaderProgram getSdfTextShader() { return SDF_TEXT; }

    @SubscribeEvent
    public static void on(RegisterShadersEvent event) {
        event.registerShader(SDF_TEXT);
    }
}
