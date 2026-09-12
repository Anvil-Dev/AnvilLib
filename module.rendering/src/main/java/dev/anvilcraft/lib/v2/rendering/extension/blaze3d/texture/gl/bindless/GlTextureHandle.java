package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl.bindless;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.TextureHandle;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record GlTextureHandle(long handleId, boolean isTexture) implements TextureHandle {
}
