package dev.anvilcraft.lib.v2.rendering.postprocess;

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
 * Holder for the custom {@link ShaderInstance}s used by the post-processing pipeline
 * (bloom downsample/upsample/apply, gaussian blur, glitch).
 * <p>
 * Ported from 26.1: the original declared the shaders as {@code RenderPipeline}s through
 * {@code RegisterRenderPipelinesEvent}; on 1.21.1 they are registered as core shaders through
 * {@link RegisterShadersEvent} (same pattern as the wheel module's {@code LibShaders}).
 * The std140 uniform blocks are bound manually through {@link GlUniformBuffer}.
 */
@ApiStatus.Internal
@Slf4j
public final class ALRPostEffectShaders {
    @Getter
    private static @Nullable ShaderInstance downSampleShader;
    @Getter
    private static @Nullable ShaderInstance upSampleShader;
    @Getter
    private static @Nullable ShaderInstance applyBloomShader;
    @Getter
    private static @Nullable ShaderInstance blurShader;
    @Getter
    private static @Nullable ShaderInstance glitchShader;

    private ALRPostEffectShaders() {
    }

    public static void register(RegisterShadersEvent event) {
        try {
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(), AnvilLibRendering.location("down_sample"), DefaultVertexFormat.POSITION_TEX),
                it -> downSampleShader = it
            );
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(), AnvilLibRendering.location("up_sample"), DefaultVertexFormat.POSITION_TEX),
                it -> upSampleShader = it
            );
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(), AnvilLibRendering.location("apply_bloom"), DefaultVertexFormat.POSITION_TEX),
                it -> applyBloomShader = it
            );
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(), AnvilLibRendering.location("blur"), DefaultVertexFormat.POSITION_TEX),
                it -> blurShader = it
            );
            event.registerShader(
                new ShaderInstance(event.getResourceProvider(), AnvilLibRendering.location("glitch"), DefaultVertexFormat.POSITION_TEX),
                it -> glitchShader = it
            );
        } catch (IOException e) {
            log.error("Failed to register post-processing shaders", e);
        }
    }
}
