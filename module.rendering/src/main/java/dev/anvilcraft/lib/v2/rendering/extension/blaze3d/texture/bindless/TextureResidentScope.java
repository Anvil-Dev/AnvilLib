package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless;

/// try-with-resources style api for {@link BindlessTexturingSupport#alrTextureHandleMakeResident(TextureHandle, boolean, boolean)}
/// and {@link BindlessTexturingSupport#alrTextureHandleDeleteResident(TextureHandle)}
public interface TextureResidentScope extends AutoCloseable {

    /// Called by GpuDevice
    void onCreate();

    @Override
    void close();
}
