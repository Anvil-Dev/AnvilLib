package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl.bindless;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.NamedUniformAccess;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.ExtendedGpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.BindlessTexturingSupport;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.TextureHandle;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.TextureResidentScope;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl.GlExtendedTextureConstants;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.ARBBindlessTexture;
import org.lwjgl.opengl.GL46;

import java.util.List;

@ApiStatus.Internal
public class GlBindlessTexturingSupport implements BindlessTexturingSupport {

    private final Object2ReferenceMap<TextureCacheKey, GlTextureHandle> textureHandleCache = new Object2ReferenceLinkedOpenHashMap<>();
    private final Object2ReferenceMap<ImageCacheKey, GlTextureHandle> imageHandleCache = new Object2ReferenceLinkedOpenHashMap<>();
    private final ALRGpuDeviceBackendExtension backendExtension;

    public GlBindlessTexturingSupport(ALRGpuDeviceBackendExtension backendExtension) {
        this.backendExtension = backendExtension;
    }

    @Override
    public TextureHandle alrCreateTextureHandle(GpuTexture texture, GpuSampler sampler) {
        GlTexture glTexture = (GlTexture) texture;
        GlSampler glSampler = (GlSampler) sampler;

        TextureCacheKey cacheKey = new TextureCacheKey(texture, sampler);
        GlTextureHandle handle = textureHandleCache.get(cacheKey);
        if (handle == null) {
            long handleId = ARBBindlessTexture.glGetTextureSamplerHandleARB(glTexture.glId(), glSampler.getId());
            handle = new GlTextureHandle(handleId, true);
            textureHandleCache.put(cacheKey, handle);
        }

        return handle;
    }

    @Override
    public TextureHandle alrCreateImageHandle(GpuTexture texture, int level, boolean layered, int layer) {
        GlTexture glTexture = (GlTexture) texture;

        ImageCacheKey cacheKey = new ImageCacheKey(texture, level, layered, layer);
        GlTextureHandle handle = this.imageHandleCache.get(cacheKey);

        int format = GlConst.toGlInternalId(glTexture.getFormat());

        if (glTexture instanceof ExtendedGpuTexture ext) {
            format = GlExtendedTextureConstants.toGlConst(ext.getActualFormat());
        }

        if (handle == null) {
            long handleId = ARBBindlessTexture.glGetImageHandleARB(glTexture.glId(), level, layered, layer, format);
            handle = new GlTextureHandle(handleId, false);
            imageHandleCache.put(cacheKey, handle);
        }

        return handle;
    }

    @Override
    public TextureResidentScope alrTextureHandleCreateResidentScope(TextureHandle handle, boolean write, boolean read) {
        GlTextureResidentScope scope = new GlTextureResidentScope(this, handle, write, read);
        scope.onCreate();
        return scope;
    }

    @Override
    public void alrTextureHandleMakeResident(TextureHandle handle, boolean write, boolean read) {
        if (handle.isTexture()) {
            ARBBindlessTexture.glMakeTextureHandleResidentARB(handleId(handle));
            return;
        }
        int access = 0;

        if (write && read) {
            access |= GL46.GL_READ_WRITE;
        } else {
            if (write) {
                access |= GL46.GL_WRITE_ONLY;
            }
            if (read) {
                access |= GL46.GL_READ_ONLY;
            }
        }

        if (access == 0) {
            throw new IllegalArgumentException(
                "alrTextureHandleMakeResident does not allow write == false && read == false"
            );
        }

        ARBBindlessTexture.glMakeImageHandleResidentARB(handleId(handle), access);
    }

    @Override
    public void alrTextureHandleDeleteResident(TextureHandle handle) {
        if (handle.isTexture()) {
            ARBBindlessTexture.glMakeTextureHandleNonResidentARB(handleId(handle));
            return;
        }

        ARBBindlessTexture.glMakeImageHandleNonResidentARB(handleId(handle));
    }

    @Override
    public void alrBindTextureHandle(NamedUniformAccess namedUniformAccess, String name, TextureHandle handle) {
        int uniformLocation = namedUniformAccess.getUniformLocation(name, this.backendExtension);
        ARBBindlessTexture.glUniformHandleui64ARB(uniformLocation, handleId(handle));
    }

    @Override
    public void alrBindTextureHandleMultiple(
        NamedUniformAccess namedUniformAccess,
        String name,
        List<TextureHandle> handle
    ) {
        int uniformLocation = namedUniformAccess.getUniformLocation(name, this.backendExtension);

        long[] handles = new long[handle.size()];
        int i = 0;
        for (TextureHandle textureHandle : handle) {
            handles[i++] = handleId(textureHandle);
        }

        ARBBindlessTexture.glUniformHandleui64vARB(uniformLocation, handles);
    }

    @SuppressWarnings("resource")
    @Override
    public void alrTextureDisposed(GpuTexture texture) {
        this.textureHandleCache.keySet().removeIf(key -> key.texture() == texture);
        this.imageHandleCache.keySet().removeIf(key -> key.texture() == texture);
    }

    private static long handleId(TextureHandle handle) {
        return ((GlTextureHandle) (handle)).handleId();
    }

    private record TextureCacheKey(
        GpuTexture texture, GpuSampler sampler
    ) {
    }

    private record ImageCacheKey(
        GpuTexture texture, int level, boolean layered, int layer
    ) {
    }
}
