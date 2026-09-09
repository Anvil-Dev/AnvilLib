package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.TextureFormat;
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
        int id
    ) {
        // use RGBA8 here hopefully make most of blaze3d code work as they don't care pixel size of actual format
        // as we are not using this as a framebuffer attachment
        super(
            usage,
            label,
            TextureFormat.RGBA8,
            width,
            height,
            depthOrLayers,
            mipLevels,
            id
        );
        this.actualFormat = format;
    }
}
