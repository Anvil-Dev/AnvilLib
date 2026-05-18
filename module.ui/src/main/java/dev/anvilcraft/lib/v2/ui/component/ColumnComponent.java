package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vertical linear layout. Children are measured and stacked top-to-bottom.
 */
public class ColumnComponent implements UIComponent {

    private final Modifier modifier;
    private List<UIComponent> children = Collections.emptyList();
    private List<MeasuredSize> childSizes = Collections.emptyList();

    // layout state
    private float x, y, width, height;

    public ColumnComponent(Modifier modifier) {
        this.modifier = modifier;
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }

    @Override
    public Modifier modifier() {
        return modifier;
    }

    @Override
    public List<UIComponent> children() {
        return children;
    }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        if (children.isEmpty()) {
            return MeasuredSize.ZERO;
        }

        float totalHeight = 0;
        float maxWidth = 0;
        List<MeasuredSize> sizes = new ArrayList<>(children.size());

        Constraints childConstraints = new Constraints(
                constraints.minWidth(), constraints.maxWidth(),
                0, Float.MAX_VALUE
        );

        for (UIComponent child : children) {
            MeasuredSize size = child.measure(childConstraints);
            sizes.add(size);
            totalHeight += size.height();
            maxWidth = Math.max(maxWidth, size.width());
        }

        this.childSizes = sizes;
        return MeasuredSize.of(
                constraints.constrainWidth(maxWidth),
                constraints.constrainHeight(totalHeight)
        );
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        float currentY = y;
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            MeasuredSize size = childSizes.get(i);
            child.layout(x, currentY, width, size.height());
            currentY += size.height();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        for (UIComponent child : children) {
            child.extractRenderState(extractor);
        }
    }
}
