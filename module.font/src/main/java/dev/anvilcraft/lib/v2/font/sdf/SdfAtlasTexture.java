package dev.anvilcraft.lib.v2.font.sdf;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uploads CPU-generated glyph atlases to GPU textures and returns bindable identifiers.
 */
public final class SdfAtlasTexture {
    private static final Map<String, TextureEntry> CACHE = new ConcurrentHashMap<>();

    private SdfAtlasTexture() {
    }

    public static Identifier getOrUpload(SdfGlyphAtlas atlas) {
        String key = atlas.key();
        int hash = hashImage(atlas.atlasImage());

        TextureEntry entry = CACHE.get(key);
        if (entry != null && entry.hash == hash) {
            return entry.id;
        }

        Identifier id = Identifier.fromNamespaceAndPath("anvillib_font", "dynamic/sdf_atlas/" + sanitize(key));
        DynamicTexture dynamicTexture = new DynamicTexture(() -> "AnvilLib SDF Atlas", toNativeImage(atlas.atlasImage()));
        Minecraft.getInstance().getTextureManager().register(id, dynamicTexture);

        if (entry != null) {
            entry.texture.close();
        }
        CACHE.put(key, new TextureEntry(id, dynamicTexture, hash));
        return id;
    }

    private static String sanitize(String key) {
        StringBuilder builder = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '_' || c == '-') {
                builder.append(c);
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                builder.append((char) (c + ('a' - 'A')));
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    private static NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                nativeImage.setPixel(x, y, image.getRGB(x, y));
            }
        }
        return nativeImage;
    }

    private static int hashImage(BufferedImage image) {
        int hash = 1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                hash = 31 * hash + image.getRGB(x, y);
            }
        }
        return hash;
    }

    private static final class TextureEntry {
        private final Identifier id;
        private final DynamicTexture texture;
        private final int hash;

        private TextureEntry(Identifier id, DynamicTexture texture, int hash) {
            this.id = id;
            this.texture = texture;
            this.hash = hash;
        }
    }
}


