package dev.anvilcraft.lib.v2.rendering.sdf;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holder for the custom {@link ShaderInstance} used by {@link SdfGraphics}.
 * <p>
 * Ported from 26.1: the original declared the SDF pipeline through {@code RenderPipeline}
 * ({@code RegisterRenderPipelinesEvent}); on 1.21.1 it is registered as a core shader through
 * {@link RegisterShadersEvent} (same pattern as the wheel module's {@code LibShaders}).
 * The {@code layout(std140) uniform SDFParameters} block is fed by a self-managed GL UBO
 * (see {@link SdfGraphics}), bound through {@code glBindBufferRange} at draw time.
 */
@ApiStatus.Internal
@Slf4j
public final class SdfShaders {
    @Getter
    private static @Nullable ShaderInstance sdfGraphicsShader;

    private SdfShaders() {
    }

    public static void register(RegisterShadersEvent event) {
        try {
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    AnvilLibRendering.location("sdf_graphics"),
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
                ),
                it -> sdfGraphicsShader = it
            );
        } catch (IOException e) {
            log.error("Failed to register sdf_graphics shader", e);
        }
    }
}
