package dev.anvilcraft.lib.v2.ui.flexible;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;

/**
 * 矩形按钮渲染器：按当前主题、样式、类型与变体取九宫格切图铺满给定区域，并在文本偏移处绘制标签。
 *
 * <p>文本颜色取自文本组件自带的样式颜色，缺省为白色。
 */
public class ButtonRenderer extends Renderer<ButtonRenderer.Scope> {
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
        protected void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
            FlexibleSprites.SpriteInfo info = FlexibleSprites.pickSprite(
                ButtonRenderer.this.theme,
                ButtonRenderer.this.style,
                ButtonRenderer.this.type,
                ButtonRenderer.this.variant.apply(this.self())
            );
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                info.sprite(),
                x,
                y,
                width,
                height
            );
            if (this.font != null && this.text != null) {
                int textX = x + info.leftPadding() + 1;
                int textY = y + info.topPadding() + 1;
                if (this.textOffset != null) {
                    textX += this.textOffset.x;
                    textY += this.textOffset.y;
                }
                graphics.text(
                    this.font,
                    this.text,
                    textX,
                    textY,
                    Optional.ofNullable(this.text.getStyle().getColor()).map(TextColor::getValue).orElse(0xFFFFFF)
                );
            }
        }

        @Override
        protected String validate(String missing) {
            return "";
        }
    }
}
