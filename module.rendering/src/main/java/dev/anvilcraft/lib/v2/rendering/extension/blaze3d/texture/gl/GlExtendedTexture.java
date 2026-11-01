package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl;

import com.mojang.renderpearl.backend.opengl.GlTexture;
import com.mojang.renderpearl.backend.opengl.FrameBufferCache;
import com.mojang.renderpearl.api.GpuFormat;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ExtendedTextureFormat;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.ExtendedGpuTexture;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

/// A simple texture implementation with extended formats
@ApiStatus.Internal
public class GlExtendedTexture extends GlTexture implements ExtendedGpuTexture {

    @Getter
    private final ExtendedTextureFormat actualFormat;

    public GlExtendedTexture(
        @Usage int usage,
        String label,
        ExtendedTextureFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels,
        int id,
        FrameBufferCache frameBufferCache
    ) {
        super(
            usage,
            label,
            switch (format) {
                case R32F -> GpuFormat.R32_FLOAT;
                case RGBA16F -> GpuFormat.RGBA16_FLOAT;
                case RGBA32F -> GpuFormat.RGBA32_FLOAT;
                case RGB10_A2 -> GpuFormat.RGB10A2_UNORM;
            },
            width,
            height,
            depthOrLayers,
            mipLevels,
            id,
            frameBufferCache
        );
        this.actualFormat = format;
    }
}
