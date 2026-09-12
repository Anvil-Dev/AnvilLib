package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl.bindless;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.TextureHandle;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.TextureResidentScope;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class GlTextureResidentScope implements TextureResidentScope {
    private final GlBindlessTexturingSupport owner;
    private final TextureHandle handle;
    private final boolean write;
    private final boolean read;

    public GlTextureResidentScope(
        GlBindlessTexturingSupport owner,
        TextureHandle handle,
        boolean write,
        boolean read
    ) {
        this.owner = owner;
        this.handle = handle;
        this.write = write;
        this.read = read;
    }

    @Override
    public void onCreate() {
        this.owner.alrTextureHandleMakeResident(this.handle, this.write, this.read);
    }

    @Override
    public void close() {
        this.owner.alrTextureHandleDeleteResident(this.handle);
    }
}
