package dev.anvilcraft.lib.v2.rendering.extension.blaze3d;

import net.neoforged.neoforge.internal.NonExhaustiveEnum;

@SuppressWarnings("UnstableApiUsage")
@NonExhaustiveEnum(reason = "Additional texture formats may be added")
public enum ExtendedTextureFormat {
    /// Single-channel 32-bit float, GL_R32F. Depth/Hi-Z pipelines.
    R32F(4),
    /// Four-channel 16-bit float, GL_RGBA16F. Common HDR color target.
    RGBA16F(8),
    /// Four-channel 32-bit float, GL_RGBA32F
    RGBA32F(16),
    /// 10-bit RGB with 2-bit alpha, GL_RGB10_A2
    RGB10_A2(4);

    private final int pixelSize;

    ExtendedTextureFormat(int pixelSize) {
        this.pixelSize = pixelSize;
    }

    public int pixelSize() {
        return this.pixelSize;
    }
}