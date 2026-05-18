package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

/**
 * 单行文字渲染。默认样式与原版一致：白色带阴影、左对齐。
 */
@Accessors(fluent = true)
public class TextComponent implements UIComponent {

    private static final int VANILLA_TEXT_COLOR = 0xFFFFFFFF;
    @Getter
    @Setter
    private Modifier modifier;
    @Setter
    private String text;
    @Setter
    private int color = VANILLA_TEXT_COLOR;
    @Setter
    private boolean shadow;
    @Setter
    private Align align = Align.LEFT;
    private float x, y, width, height;

    public TextComponent(Modifier modifier, String text) {
        this.modifier = modifier;
        this.text = text;
    }

    @Override
    public List<UIComponent> children() {
        return Collections.emptyList();
    }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        float w = font.width(Component.literal(text));
        float h = font.lineHeight;
        return MeasuredSize.of(constraints.constrainWidth(w), constraints.constrainHeight(h));
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
        var font = Minecraft.getInstance().font;
        Component component = Component.literal(text);
        float textW = font.width(component);

        float renderX = (float) switch (align) {
            case LEFT -> x;
            case CENTER -> x + (width - textW) / 2f;
            case RIGHT -> x + width - textW;
        };

        extractor.text(font, text, (int) renderX, (int) y, color, this.shadow);
    }

    /**
     * 文字水平对齐方式
     */
    public enum Align {LEFT, CENTER, RIGHT}
}

