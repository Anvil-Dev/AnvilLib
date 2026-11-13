package dev.anvilcraft.lib.v2.font.sdf;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

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
    public static ResourceLocation uploadPage(SdfGlyphAtlas atlas, int pageIndex) {
        SdfGlyphPage page = atlas.page(pageIndex);
        String key = atlas.key() + ".p" + pageIndex;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("anvillib_font", "dynamic/sdf_atlas/" + sanitize(key));
        PageEntry entry = CACHE.get(key);
        boolean create = entry == null || entry.texture.closed;
        if (!create && entry.version == page.version.get() && !page.dirty) return entry.id;
        SdfGlyphPage.Upload upload = page.snapshot(create);
        if (upload == null) return id;
        if (create) {
            // DynamicTexture owns the initial image and releases it when the texture closes.
            SdfTexture texture = new SdfTexture(toNativeImage(upload));
            Minecraft.getInstance().getTextureManager().register(id, texture);
            entry = new PageEntry(id, texture, upload.version());
            CACHE.put(key, entry);
        } else {
            try (NativeImage image = toNativeImage(upload)) {
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
    static final class SdfTexture extends DynamicTexture {
        private boolean closed;
        SdfTexture(NativeImage image) {
            super(image);
            this.setFilter(true, false);
        }

        void upload(NativeImage image, int x, int y) {
            NativeImage backing = this.getPixels();
            image.copyRect(backing, 0, 0, x, y, image.getWidth(), image.getHeight(), false, false);
            this.bind();
            backing.upload(0, x, y, x, y, image.getWidth(), image.getHeight(), true, true, false, false);
        }

        @Override
        public void close() {
            this.closed = true;
            super.close();
        }
    }

    static NativeImage toNativeImage(SdfGlyphPage.Upload upload) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, upload.width(), upload.height(), false);
        for (int y = 0; y < upload.height(); y++) {
            for (int x = 0; x < upload.width(); x++) {
                int gray = upload.pixels()[y * upload.width() + x] & 0xFF;
                image.setPixelRGBA(x, y, 0xFF000000 | (gray << 16) | (gray << 8) | gray);
            }
        }
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
        final ResourceLocation id;
        final SdfTexture texture;
        int version;

        PageEntry(ResourceLocation id, SdfTexture texture, int version) {
            this.id = id;
            this.texture = texture;
            this.version = version;
        }
    }
}
