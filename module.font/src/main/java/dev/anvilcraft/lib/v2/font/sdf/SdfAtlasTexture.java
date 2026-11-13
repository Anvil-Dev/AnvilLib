package dev.anvilcraft.lib.v2.font.sdf;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uploads SDF glyph atlas pages to GPU textures with LINEAR filtering.
 */
@ApiStatus.Internal
public final class SdfAtlasTexture {
    private static final Map<String, PageEntry> CACHE = new ConcurrentHashMap<>();

    private SdfAtlasTexture() {
    }

    /**
     * Upload a single atlas page to the GPU, returning its texture location.
     */
    public static Identifier uploadPage(SdfGlyphAtlas atlas, int pageIndex) {
        SdfGlyphPage page = atlas.page(pageIndex);
        String key = atlas.key() + ".p" + pageIndex;
        Identifier id = Identifier.fromNamespaceAndPath("anvillib_font", "dynamic/sdf_atlas/" + sanitize(key));
        PageEntry entry = CACHE.get(key);
        boolean create = entry == null || entry.texture.closed;
        if (!create && entry.version == page.version.get() && !page.dirty) return entry.id;
        SdfGlyphPage.Upload upload = page.snapshot(create);
        if (upload == null) return id;
        try (NativeImage image = toNativeImage(upload)) {
            if (create) {
                SdfTexture texture = new SdfTexture(image);
                Minecraft.getInstance().getTextureManager().register(id, texture);
                entry = new PageEntry(id, texture, upload.version());
                CACHE.put(key, entry);
            } else {
                entry.texture.upload(image, upload.x(), upload.y());
                entry.version = upload.version();
            }
        }
        page.textureId = id;
        page.uploaded(upload);
        return id;
    }

    /**
     * Upload all dirty pages of an atlas. Called before rendering.
     */
    public static void ensureUploaded(SdfGlyphAtlas atlas) {
        for (int i = 0; i < atlas.pageCount(); i++) {
            SdfGlyphPage page = atlas.page(i);
            String key = atlas.key() + ".p" + i;
            PageEntry entry = CACHE.get(key);
            if (page.textureId == null
                || entry == null
                || entry.texture.closed
                || entry.version != page.version.get()
                || page.dirty) {
                uploadPage(atlas, i);
            }
        }
    }

    /**
     * A minimal single-channel texture with LINEAR+CLAMP filtering for SDF sampling.
     */
    static final class SdfTexture extends AbstractTexture {
        private boolean closed;
        SdfTexture(NativeImage image) {
            GpuDevice device = RenderSystem.getDevice();
            this.texture = device.createTexture(
                () -> "AnvilLib SDF Atlas", 5, TextureFormat.RED8,
                image.getWidth(), image.getHeight(), 1, 1
            );
            device.createCommandEncoder().writeToTexture(this.texture, image);
            this.textureView = device.createTextureView(this.texture);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }

        void upload(NativeImage image, int x, int y) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(
                this.texture, image, 0, 0, x, y, image.getWidth(), image.getHeight(), 0, 0
            );
        }

        @Override
        public void close() {
            this.closed = true;
            super.close();
        }
    }

    static NativeImage toNativeImage(SdfGlyphPage.Upload upload) {
        NativeImage image = new NativeImage(NativeImage.Format.LUMINANCE, upload.width(), upload.height(), false);
        MemoryUtil.memByteBuffer(image.getPointer(), upload.pixels().length).put(upload.pixels());
        return image;
    }

    private static String sanitize(String key) {
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                sb.append((char) (c + ('a' - 'A')));
            } else if (c == '#') {
                sb.append('_');
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static final class PageEntry {
        final Identifier id;
        final SdfTexture texture;
        int version;

        PageEntry(Identifier id, SdfTexture texture, int version) {
            this.id = id;
            this.texture = texture;
            this.version = version;
        }
    }
}
