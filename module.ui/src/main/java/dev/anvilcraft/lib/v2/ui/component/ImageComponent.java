package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.List;

/**
 * 渲染一个材质精灵（sprite）。
 * 通过 {@link GuiGraphicsExtractor#blitSprite} 使用原版纹理管线。
 */
public class ImageComponent implements UIComponent {

    private final Modifier modifier;
    private Identifier sprite;
    private float imageWidth;
    private float imageHeight;

    private float x, y, width, height;

    public ImageComponent(Modifier modifier, Identifier sprite, float imageWidth, float imageHeight) {
        this.modifier = modifier;
        this.sprite = sprite;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public ImageComponent sprite(Identifier sprite) { this.sprite = sprite; return this; }

    @Override public Modifier modifier() { return modifier; }
    @Override public List<UIComponent> children() { return Collections.emptyList(); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        return MeasuredSize.of(
                constraints.constrainWidth(imageWidth),
                constraints.constrainHeight(imageHeight)
        );
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        extractor.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                sprite,
                (int) x, (int) y,
                (int) width, (int) height
        );
    }

    /** 获取组件逻辑宽度（可能被 modifier 修改后不同）。 */
    float imageWidth() { return imageWidth; }
    /** 获取组件逻辑高度。 */
    float imageHeight() { return imageHeight; }
}
