package dev.anvilcraft.lib.v2.ui.flexible;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;

import java.util.Optional;

/**
 * 矩形按钮渲染器：按当前主题、样式、类型与变体取九宫格切图铺满给定区域，并在文本偏移处绘制标签。
 *
 * <p>文本颜色取自文本组件自带的样式颜色，缺省为白色；两者都补齐不透明 alpha，
 * 因为 {@link TextColor#getValue()} 只带 RGB，alpha 为 0 时文本不会被绘制。
 * 渲染器设置了整体透明度时，本体与文本都按该透明度着色。
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
        return ButtonRenderer.textColor(text) & 0xFFFFFF | ButtonRenderer.alphaBits(alpha);
    }

    /**
     * 按整体透明度算出 alpha 分量，供文本颜色组合使用。
     */
    public static int alphaBits(float alpha) {
        return (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24;
    }

    /**
     * 整体透明度不为 1 时把当前着色器颜色调暗，九宫格切图没有逐次绘制的颜色参数，只能靠着色器颜色乘算。
     */
    public static void applyAlpha(GuiGraphics graphics, float alpha) {
        if (alpha >= 1.0F) {
            return;
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, Mth.clamp(alpha, 0.0F, 1.0F));
    }

    /**
     * 恢复着色器颜色，必须与 {@link #applyAlpha(GuiGraphics, float)} 成对调用。
     */
    public static void resetAlpha(GuiGraphics graphics, float alpha) {
        if (alpha >= 1.0F) {
            return;
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
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
            ButtonRenderer.applyAlpha(graphics, ButtonRenderer.this.alpha);
            graphics.blitSprite(
                info.sprite(),
                x,
                y,
                width,
                height
            );
            ButtonRenderer.resetAlpha(graphics, ButtonRenderer.this.alpha);
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
