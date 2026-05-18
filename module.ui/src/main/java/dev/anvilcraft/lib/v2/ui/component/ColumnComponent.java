package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 纵向线性布局。子组件自上而下排列。主轴=垂直，交叉轴=水平。
 */
public class ColumnComponent implements UIComponent {

    private Modifier modifier;
    private List<UIComponent> children = Collections.emptyList();
    private List<MeasuredSize> childSizes = Collections.emptyList();

    private Arrangement.Vertical verticalArrangement = Arrangement.Vertical.Top;
    private Alignment.Horizontal horizontalAlignment = Alignment.Horizontal.Start;
    private float spacing;

    // 布局状态
    private float x, y, width, height;

    public ColumnComponent(Modifier modifier) {
        this.modifier = modifier;
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }

    // ── 链式 setter ──

    public ColumnComponent verticalArrangement(Arrangement.Vertical va) {
        this.verticalArrangement = va;
        return this;
    }

    public ColumnComponent horizontalAlignment(Alignment.Horizontal ha) {
        this.horizontalAlignment = ha;
        return this;
    }

    public ColumnComponent spacing(float spacing) {
        this.spacing = spacing;
        return this;
    }

    public ColumnComponent modifier(Modifier m) {
        this.modifier = m;
        return this;
    }

    @Override
    public Modifier modifier() { return modifier; }

    @Override
    public List<UIComponent> children() { return children; }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        if (children.isEmpty()) return MeasuredSize.ZERO;

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
        totalHeight += spacing * (children.size() - 1);

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

        List<Float> heights = new ArrayList<>(childSizes.size());
        for (MeasuredSize s : childSizes) heights.add(s.height());

        float[] yOffsets = verticalArrangement.arrange(height, heights, spacing);
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            MeasuredSize size = childSizes.get(i);
            float childX = x + horizontalAlignment.align(width, size.width());
            child.layout(childX, y + yOffsets[i], size.width(), size.height());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        for (UIComponent child : children) {
            child.extractRenderState(extractor);
        }
    }
}

