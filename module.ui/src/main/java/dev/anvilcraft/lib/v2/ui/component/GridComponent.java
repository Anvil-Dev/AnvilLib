package dev.anvilcraft.lib.v2.ui.component;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 网格布局。子组件按列数排列，每格大小由最大子组件决定。
 */
@Accessors(fluent = true)
public class GridComponent implements UIComponent {

    @Getter @Setter
    private Modifier modifier;
    private final int columns;
    @Getter
    private List<UIComponent> children = Collections.emptyList();
    private List<MeasuredSize> childSizes = Collections.emptyList();
    private float hSpacing, vSpacing;

    private float x, y, width, height;
    private float cellW, cellH;

    public GridComponent(Modifier modifier, int columns) {
        this.modifier = modifier;
        this.columns = Math.max(1, columns);
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }

    public GridComponent spacing(float h, float v) { this.hSpacing = h; this.vSpacing = v; return this; }

    public MeasuredSize measure(Constraints constraints) {
        if (children.isEmpty()) return MeasuredSize.ZERO;

        float maxW = 0, maxH = 0;
        List<MeasuredSize> sizes = new ArrayList<>(children.size());
        Constraints childC = new Constraints(0, Float.MAX_VALUE, 0, Float.MAX_VALUE);

        for (UIComponent child : children) {
            MeasuredSize s = child.measure(childC);
            sizes.add(s);
            maxW = Math.max(maxW, s.width());
            maxH = Math.max(maxH, s.height());
        }

        this.childSizes = sizes;
        this.cellW = maxW;
        this.cellH = maxH;

        int rows = (children.size() + columns - 1) / columns;
        float totalW = maxW * columns + hSpacing * (columns - 1);
        float totalH = maxH * rows + vSpacing * (rows - 1);

        return MeasuredSize.of(constraints.constrainWidth(totalW), constraints.constrainHeight(totalH));
    }

    public void layout(float x, float y, float width, float height) {
        this.x = x; this.y = y; this.width = width; this.height = height;

        for (int i = 0; i < children.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            float cx = x + col * (cellW + hSpacing);
            float cy = y + row * (cellH + vSpacing);
            children.get(i).layout(cx, cy, cellW, cellH);
        }
    }

    public void extractRenderState(GuiGraphicsExtractor extractor) {
        for (UIComponent child : children) child.extractRenderState(extractor);
    }
}
