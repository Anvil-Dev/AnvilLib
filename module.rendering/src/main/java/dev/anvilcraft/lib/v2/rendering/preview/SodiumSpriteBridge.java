package dev.anvilcraft.lib.v2.rendering.preview;

import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;

/** 仅在 Sodium 已安装时通知其更新预览中使用的动画纹理。 */
public final class SodiumSpriteBridge {
    private SodiumSpriteBridge() {
    }

    public static void pingSpriteUsage(@Nullable TextureAtlasSprite sprite) {
        if (sprite != null && ModList.get().isLoaded("sodium")) {
            SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }
    }
}
