package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 横向线性布局。子组件自左而右排列。主轴=水平，交叉轴=垂直。
 */
public class RowComponent implements UIComponent {

    private final Modifier modifier;
    private List<UIComponent> children = Collections.emptyList();
    private List<MeasuredSize> childSizes = Collections.emptyList();

    private Arrangement.Horizontal horizontalArrangement = Arrangement.Horizontal.Start;
    private Alignment.Vertical verticalAlignment = Alignment.Vertical.Top;
    private float spacing;

    private float x, y, width, height;

    public RowComponent(Modifier modifier) {
        this.modifier = modifier;
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }

    public RowComponent horizontalArrangement(Arrangement.Horizontal ha) {
        this.horizontalArrangement = ha;
        return this;
    }

    public RowComponent verticalAlignment(Alignment.Vertical va) {
        this.verticalAlignment = va;
        return this;
    }

    public RowComponent spacing(float spacing) {
        this.spacing = spacing;
        return this;
    }

    @Override
    public Modifier modifier() { return modifier; }

    @Override
    public List<UIComponent> children() { return children; }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        if (children.isEmpty()) return MeasuredSize.ZERO;

        float totalWidth = 0;
        float maxHeight = 0;
        List<MeasuredSize> sizes = new ArrayList<>(children.size());

        Constraints childConstraints = new Constraints(
                0, Float.MAX_VALUE,
                constraints.minHeight(), constraints.maxHeight()
        );

        for (UIComponent child : children) {
            MeasuredSize size = child.measure(childConstraints);
            sizes.add(size);
            totalWidth += size.width();
            maxHeight = Math.max(maxHeight, size.height());
        }
        totalWidth += spacing * (children.size() - 1);

        this.childSizes = sizes;
        return MeasuredSize.of(
                constraints.constrainWidth(totalWidth),
                constraints.constrainHeight(maxHeight)
        );
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        List<Float> widths = new ArrayList<>(childSizes.size());
        for (MeasuredSize s : childSizes) widths.add(s.width());

        float[] xOffsets = horizontalArrangement.arrange(width, widths, spacing);
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            MeasuredSize size = childSizes.get(i);
            float childY = y + verticalAlignment.align(height, size.height());
            child.layout(x + xOffsets[i], childY, size.width(), size.height());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        for (UIComponent child : children) {
            child.extractRenderState(extractor);
        }
    }
}
