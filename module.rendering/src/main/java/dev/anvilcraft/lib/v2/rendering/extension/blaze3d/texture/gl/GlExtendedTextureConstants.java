package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ExtendedTextureFormat;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL46;

@ApiStatus.Internal
public class GlExtendedTextureConstants {
    public static int toGlConst(ExtendedTextureFormat textureFormat){
        return switch (textureFormat) {
            case R32F -> GL46.GL_R32F;
            case RGBA16F -> GL46.GL_RGBA16F;
            case RGBA32F -> GL46.GL_RGBA32F;
            case RGB10_A2 -> GL46.GL_RGB10_A2;
        };
    }

    public static int toGlInternalId(ExtendedTextureFormat format) {
        return toGlConst(format);
    }

    public static int toGlExternalId(ExtendedTextureFormat format) {
        return switch (format) {
            case R32F -> GL46.GL_RED;
            case RGBA16F, RGBA32F, RGB10_A2 -> GL46.GL_RGBA;
        };
    }

    public static int toGlType(ExtendedTextureFormat format) {
        return switch (format) {
            case R32F, RGBA32F -> GL46.GL_FLOAT;
            case RGBA16F -> GL46.GL_HALF_FLOAT;
            case RGB10_A2 -> GL46.GL_UNSIGNED_INT_2_10_10_10_REV;
        };
    }
}
