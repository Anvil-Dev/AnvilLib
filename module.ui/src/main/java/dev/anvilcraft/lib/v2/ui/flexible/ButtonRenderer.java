package dev.anvilcraft.lib.v2.ui.flexible;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * 矩形按钮渲染器：按当前主题、样式、类型与变体取九宫格切图铺满给定区域，并在文本偏移处绘制标签。
 *
 * <p>文本颜色取自文本组件自带的样式颜色，缺省为白色；两者都补齐不透明 alpha，
 * 因为 {@link TextColor#getValue()} 只带 RGB，alpha 为 0 时文本不会被绘制。
 * 渲染器设置了整体透明度时，文本按该透明度着色；九宫格本体没有逐次绘制的颜色参数，
 * 改由着色器颜色乘算透明度。
 */
public class ButtonRenderer extends Renderer<ButtonRenderer.Scope> {
    /**
     * 按文本组件自带的样式颜色计算不透明 ARGB 颜色，缺省为白色。
     */
    public static int textColor(Component text) {
        return Optional.ofNullable(text.getStyle().getColor())
            .map(TextColor::getValue)
            .map(value -> value | 0xFF000000)
            .orElse(0xFFFFFFFF);
    }

    /**
     * 按文本组件自带颜色与整体透明度算出 ARGB 颜色。
     */
    public static int textColor(Component text, float alpha) {
        return ButtonRenderer.textColor(text) & 0xFFFFFF | ButtonRenderer.color(alpha) & 0xFF000000;
    }

    /**
     * 按整体透明度算出白色 ARGB 颜色，供文本颜色组合与测试使用。
     */
    public static int color(float alpha) {
        return (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | 0xFFFFFF;
    }

    /**
     * 把当前着色器颜色的 alpha 分量乘算整体透明度。
     *
     * <p>九宫格切图没有逐次绘制的颜色参数，只能靠着色器颜色乘算；返回进入前的颜色，
     * 必须交给 {@link #resetAlpha(GuiGraphics, float[])} 原样恢复，否则后续界面绘制会被残留颜色染色。
     *
     * @return 进入前的着色器颜色，透明度为 1 时返回 {@code null} 表示无需恢复
     */
    public static float @Nullable [] applyAlpha(GuiGraphics graphics, float alpha) {
        if (alpha >= 1.0F) {
            return null;
        }
        float[] previous = RenderSystem.getShaderColor().clone();
        graphics.flush();
        RenderSystem.setShaderColor(
            previous[0],
            previous[1],
            previous[2],
            previous[3] * Mth.clamp(alpha, 0.0F, 1.0F)
        );
        return previous;
    }

    /**
     * 恢复 {@link #applyAlpha(GuiGraphics, float)} 保存的着色器颜色。
     */
    public static void resetAlpha(GuiGraphics graphics, float @Nullable [] previous) {
        if (previous == null) {
            return;
        }
        graphics.flush();
        RenderSystem.setShaderColor(previous[0], previous[1], previous[2], previous[3]);
    }

    @Override
    public Scope open() {
        return new Scope();
    }

    public class Scope extends Renderer<Scope>.Scope {
        @Override
        protected Scope self() {
            return this;
        }

        @Override
        protected void render(GuiGraphics graphics, int x, int y, int width, int height) {
            FlexibleSprites.SpriteInfo info = FlexibleSprites.pickSprite(
                ButtonRenderer.this.theme,
                ButtonRenderer.this.style,
                ButtonRenderer.this.type,
                ButtonRenderer.this.variant.apply(this.self())
            );
            float @Nullable [] previous = ButtonRenderer.applyAlpha(graphics, ButtonRenderer.this.alpha);
            try {
                graphics.blitSprite(
                    info.sprite(),
                    x,
                    y,
                    width,
                    height
                );
            } finally {
                ButtonRenderer.resetAlpha(graphics, previous);
            }
            if (this.font != null && this.text != null) {
                int textX = x + info.leftPadding() + 1;
                int textY = y + info.topPadding() + 1;
                if (this.textOffset != null) {
                    textX += this.textOffset.x;
                    textY += this.textOffset.y;
                }
                graphics.drawString(
                    this.font,
                    this.text,
                    textX,
                    textY,
                    ButtonRenderer.textColor(this.text, ButtonRenderer.this.alpha)
                );
            }
        }

        @Override
        protected String validate(String missing) {
            return "";
        }
    }
}
