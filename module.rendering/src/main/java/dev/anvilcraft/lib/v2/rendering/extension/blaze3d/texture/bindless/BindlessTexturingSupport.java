package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.NamedUniformAccess;

import java.util.List;

public interface BindlessTexturingSupport {

    TextureHandle alrCreateTextureHandle(GpuTexture texture, GpuSampler sampler);

    TextureHandle alrCreateImageHandle(GpuTexture texture, int level, boolean layered, int layer);

    TextureResidentScope alrTextureHandleCreateResidentScope(TextureHandle handle, boolean write, boolean read);

    default void alrTextureHandleMakeResident(TextureHandle handle) {
        this.alrTextureHandleMakeResident(handle, true, true);
    }

    void alrTextureHandleMakeResident(TextureHandle handle, boolean write, boolean read);

    void alrTextureHandleDeleteResident(TextureHandle handle);

    void alrBindTextureHandle(NamedUniformAccess namedUniformAccess, String name, TextureHandle handle);

    void alrBindTextureHandleMultiple(NamedUniformAccess namedUniformAccess, String name, List<TextureHandle> handle);

    void alrTextureDisposed(GpuTexture texture);
}
