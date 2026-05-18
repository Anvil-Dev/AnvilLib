package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Collections;
import java.util.List;

/**
 * 固定尺寸的空白占位组件，不渲染任何内容。
 */
@Accessors(fluent = true)
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class SpacerComponent implements UIComponent {
    private final float spacerWidth;
    private final float spacerHeight;
    @Getter
    @Setter
    private Modifier modifier;

    public SpacerComponent(Modifier modifier, float width, float height) {
        this.modifier = modifier;
        this.spacerWidth = width;
        this.spacerHeight = height;
    }

    @Override
    public List<UIComponent> children() {
        return Collections.emptyList();
    }


    @Override
    public MeasuredSize measure(Constraints constraints) {
        return MeasuredSize.of(
            constraints.constrainWidth(spacerWidth),
            constraints.constrainHeight(spacerHeight)
        );
    }

    @Override
    public void layout(float x, float y, float width, float height) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
    }
}
