package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Accessors(fluent = true)
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class GridComponent implements UIComponent {

    private final int columns;
    @Getter @Setter
    private Modifier modifier;
    @Getter
    private List<UIComponent> children = Collections.emptyList();
    @SuppressWarnings("FieldCanBeLocal")
    private List<MeasuredSize> childSizes = Collections.emptyList();
    private float hSpacing, vSpacing;

    @Getter
    private float x, y, width, height;
    private float cellW, cellH;

    public GridComponent(Modifier modifier, int columns) {
        this.modifier = modifier;
        this.columns = Math.max(1, columns);
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }

    public GridComponent spacing(float h, float v) {
        this.hSpacing = h;
        this.vSpacing = v;
        return this;
    }

    public MeasuredSize measure(Constraints constraints) {
        if (this.children.isEmpty()) return MeasuredSize.ZERO;

        float maxW = 0, maxH = 0;
        List<MeasuredSize> sizes = new ArrayList<>(this.children.size());
        Constraints childC = new Constraints(0, Float.MAX_VALUE, 0, Float.MAX_VALUE);

        for (UIComponent child : this.children) {
            MeasuredSize s = child.measure(childC);
            sizes.add(s);
            maxW = Math.max(maxW, s.width());
            maxH = Math.max(maxH, s.height());
        }

        this.childSizes = sizes;
        this.cellW = maxW;
        this.cellH = maxH;

        int rows = (this.children.size() + this.columns - 1) / this.columns;
        float totalW = maxW * this.columns + this.hSpacing * (this.columns - 1);
        float totalH = maxH * rows + this.vSpacing * (rows - 1);

        return MeasuredSize.of(constraints.constrainWidth(totalW), constraints.constrainHeight(totalH));
    }

    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        for (int i = 0; i < this.children.size(); i++) {
            int col = i % this.columns;
            int row = i / this.columns;
            float cx = this.x + col * (this.cellW + this.hSpacing);
            float cy = this.y + row * (this.cellH + this.vSpacing);
            this.children.get(i).layout(cx, cy, this.cellW, this.cellH);
        }
    }

    public void extractRenderState(GuiGraphicsExtractor extractor) {
        for (UIComponent child : this.sortedChildren()) child.extractRenderState(extractor);
    }
}
