package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Alignment;
import dev.anvilcraft.lib.v2.ui.Arrangement;
import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutHelper;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
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
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class ColumnComponent implements UIComponent {
    @Getter
    @Setter
    private Modifier modifier;
    @Getter
    private List<UIComponent> children = Collections.emptyList();
    private List<MeasuredSize> childSizes = Collections.emptyList();
    private List<LayoutRect> childRects = Collections.emptyList();

    @Setter
    private Arrangement.Vertical verticalArrangement = Arrangement.Vertical.Top;
    @Setter
    private Alignment.Horizontal horizontalAlignment = Alignment.Horizontal.Start;
    @Setter
    private float spacing;

    @Getter
    private float x, y, width, height;

    public ColumnComponent(Modifier modifier) {
        this.modifier = modifier;
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }

    public MeasuredSize measure(Constraints constraints) {
        if (this.children.isEmpty()) return MeasuredSize.ZERO;

        float totalHeight = 0;
        float maxWidth = 0;
        List<MeasuredSize> sizes = new ArrayList<>(this.children.size());

        Constraints childConstraints = new Constraints(
            0, constraints.maxWidth(),
            0, Float.MAX_VALUE
        );

        for (UIComponent child : this.children) {
            MeasuredSize size = LayoutHelper.measureChild(child, childConstraints);
            sizes.add(size);
            totalHeight += size.height();
            maxWidth = Math.max(maxWidth, size.width());
        }
        totalHeight += this.spacing * (this.children.size() - 1);

        this.childSizes = sizes;
        return MeasuredSize.of(
            constraints.constrainWidth(maxWidth),
            constraints.constrainHeight(totalHeight)
        );
    }

    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        List<Float> heights = new ArrayList<>(this.childSizes.size());
        for (MeasuredSize s : this.childSizes) heights.add(s.height());

        float[] yOffsets = this.verticalArrangement.arrange(this.height, heights, this.spacing);
        List<LayoutRect> rects = new ArrayList<>(this.children.size());
        for (int i = 0; i < this.children.size(); i++) {
            UIComponent child = this.children.get(i);
            MeasuredSize size = this.childSizes.get(i);
            float childX = this.x + this.horizontalAlignment.align(this.width, size.width());
            float cy = this.y + yOffsets[i];
            LayoutRect rect = LayoutRect.of(childX, cy, size.width(), size.height());
            rects.add(rect);
            LayoutHelper.layoutChild(child, childX, cy, size.width(), size.height());
        }
        this.childRects = rects;
    }

    public void extractRenderState(GuiGraphicsExtractor extractor) {
        for (int i = 0; i < this.children.size(); i++) {
            UIComponent child = this.children.get(i);
            LayoutRect r = this.childRects.get(i);
            LayoutHelper.renderChild(child, extractor, r.x(), r.y(), r.width(), r.height());
        }
    }
}
