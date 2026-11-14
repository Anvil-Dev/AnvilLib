package dev.anvilcraft.lib.v2.rendering.preview.level;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * 用于结构预览渲染的动态光照贴图构建器。
 */
public class PreviewLightmapAtlas implements AutoCloseable {
    private final DynamicTexture lightmapTexture;
    private final NativeImage lightmapPixels;

    public PreviewLightmapAtlas() {
        this.lightmapTexture = new DynamicTexture(16, 16, false);
        this.lightmapPixels = Objects.requireNonNull(this.lightmapTexture.getPixels());
        this.lightmapPixels.fillRect(0, 0, 16, 16, -1);
        this.lightmapTexture.upload();
    }

    /**
     * 近似计算当前时间下的原版天空亮度衰减。
     */
    public float computeSkyDarken(Level level, float partialTick) {
        var f = level.getTimeOfDay(partialTick);
        var g = 1.0F - (Mth.cos(f * (float) (Math.PI * 2)) * 2.0F + 0.2F);
        g = Mth.clamp(g, 0.0F, 1.0F);
        g = 1.0F - g;
        return 0.2f + g * 0.8f;
    }

    /**
     * 重算并上传 16x16 光照贴图。
     */
    public void update(Level level) {
        float f = computeSkyDarken(level, 1.0f);
        Vector3f vector3f = new Vector3f(f, f, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
        float g = f * 0.95F + 0.05F;
        float m = 1.5F;

        var minecraft = Minecraft.getInstance();

        Vector3f vector3f2 = new Vector3f();

        for (int skyLightLvl = 0; skyLightLvl < 16; ++skyLightLvl) {
            for (int blockLightLvl = 0; blockLightLvl < 16; ++blockLightLvl) {
                float p = LightTexture.getBrightness(level.dimensionType(), skyLightLvl) * g;
                float q = LightTexture.getBrightness(level.dimensionType(), blockLightLvl) * m;

                float s = q * ((q * 0.6F + 0.4F) * 0.6F + 0.4F);
                float t = q * (q * q * 0.6F + 0.4F);
                vector3f2.set(q, s, t);

                Vector3f vector3f3 = new Vector3f(vector3f).mul(p);
                vector3f2.add(vector3f3);
                vector3f2.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);

                float v = minecraft.options.gamma().get().floatValue();
                Vector3f vector3f5 = new Vector3f(notGamma(vector3f2.x), notGamma(vector3f2.y), notGamma(vector3f2.z));
                vector3f2.lerp(vector3f5, Math.max(0.0F, v));
                vector3f2.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
                clampColor(vector3f2);
                vector3f2.mul(255.0F);

                int x = (int) vector3f2.x();
                int y = (int) vector3f2.y();
                int z = (int) vector3f2.z();
                this.lightmapPixels.setPixelRGBA(blockLightLvl, skyLightLvl, 0xFF000000 | z << 16 | y << 8 | x);
            }
        }

        this.lightmapTexture.upload();
    }

    private static void clampColor(Vector3f vector3f) {
        vector3f.set(
            Mth.clamp(vector3f.x, 0.0F, 1.0F), Mth.clamp(vector3f.y, 0.0F, 1.0F),
            Mth.clamp(vector3f.z, 0.0F, 1.0F)
        );
    }

    private float notGamma(float value) {
        float f = 1.0F - value;
        return 1.0F - f * f * f * f;
    }

    /**
     * 按原版世界渲染约定，将该光照贴图绑定到纹理单元 2。
     */
    public void bind() {
        RenderSystem.setShaderTexture(2, this.lightmapTexture.getId());
        this.lightmapTexture.bind();
        RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_LINEAR);
        RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_LINEAR);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @Override
    public void close() {
        this.lightmapTexture.close();
    }
}
