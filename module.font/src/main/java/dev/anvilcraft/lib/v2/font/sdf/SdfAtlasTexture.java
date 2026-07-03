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

import java.awt.image.BufferedImage;
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
        // Synchronize on page to read the version and image data atomically
        // with respect to the glyph creation thread, which also synchronizes
        // on page for placeGlyph / fillPaddingForCell.
        NativeImage nativeImage;
        int version;
        synchronized (page) {
            version = page.version.get();
            nativeImage = toNativeImage(page.image);
            page.dirty = false;
        }

        PageEntry entry = CACHE.get(key);
        if (entry != null && entry.version == version) return entry.id;

        SdfTexture texture = new SdfTexture(nativeImage);
        Minecraft.getInstance().getTextureManager().register(id, texture);

        if (entry != null) entry.texture.close();
        CACHE.put(key, new PageEntry(id, texture, version));
        page.textureId = id;
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
    }

    static NativeImage toNativeImage(BufferedImage image) {
        NativeImage ni = new NativeImage(NativeImage.Format.RGBA, image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int gray = image.getRGB(x, y) & 0xFF;
                ni.setPixel(x, y, (0xFF << 24) | (gray << 16) | (gray << 8) | gray);
            }
        }
        return ni;
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
        final int version;

        PageEntry(Identifier id, SdfTexture texture, int version) {
            this.id = id;
            this.texture = texture;
            this.version = version;
        }
    }
}
