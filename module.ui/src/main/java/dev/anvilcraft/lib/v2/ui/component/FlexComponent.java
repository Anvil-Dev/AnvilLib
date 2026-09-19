package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Alignment;
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

/**
 * 弹性布局。子组件按权重（flexGrow）分配主轴上的剩余空间。
 * 主轴方向通过 {@link Direction} 指定。
 */
@Accessors(fluent = true)
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class FlexComponent implements UIComponent {

    public enum Direction { ROW, COLUMN }

    @Getter @Setter
    private Modifier modifier;
    @Getter
    private List<UIComponent> children = Collections.emptyList();
    private final Direction direction;
    private List<MeasuredSize> childSizes = Collections.emptyList();
    private List<Float> childFlexGrows = Collections.emptyList();
    private List<LayoutRect> childRects = Collections.emptyList();

    @Setter
    private float spacing;
    @Setter
    private Alignment.Horizontal crossAlignH = Alignment.Horizontal.Start;
    @Setter
    private Alignment.Vertical crossAlignV = Alignment.Vertical.Top;

    @Getter
    private float x, y, width, height;

    public FlexComponent(Modifier modifier, Direction direction) {
        this.modifier = modifier;
        this.direction = direction;
    }

    public void setChildren(List<UIComponent> children) { this.children = List.copyOf(children); }
    public void setChildFlexGrows(List<Float> flexGrows) { this.childFlexGrows = List.copyOf(flexGrows); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        if (this.children.isEmpty()) return MeasuredSize.ZERO;

        boolean isRow = this.direction == Direction.ROW;
        float totalMain = 0;
        float maxCross = 0;
        List<MeasuredSize> sizes = new ArrayList<>(this.children.size());
        Constraints childC = isRow
                ? new Constraints(0, Float.MAX_VALUE, 0, constraints.maxHeight())
                : new Constraints(0, constraints.maxWidth(), 0, Float.MAX_VALUE);

        for (int i = 0; i < this.children.size(); i++) {
            UIComponent child = this.children.get(i);
            MeasuredSize s = LayoutHelper.measureChild(child, childC);
            sizes.add(s);
            float w = i < this.childFlexGrows.size() ? this.childFlexGrows.get(i) : 1f;
            if (w <= 0) {
                totalMain += isRow ? s.width() : s.height();
            }
            maxCross = Math.max(maxCross, isRow ? s.height() : s.width());
        }
        totalMain += this.spacing * (this.children.size() - 1);

        this.childSizes = sizes;
        if (isRow) {
            return MeasuredSize.of(constraints.constrainWidth(totalMain), constraints.constrainHeight(maxCross));
        }
        return MeasuredSize.of(constraints.constrainWidth(maxCross), constraints.constrainHeight(totalMain));
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        if (this.children.isEmpty()) return;

        boolean isRow = this.direction == Direction.ROW;
        float totalFixed = 0;
        float totalWeight = 0;
        for (int i = 0; i < this.children.size(); i++) {
            float w = i < this.childFlexGrows.size() ? this.childFlexGrows.get(i) : 1f;
            if (w <= 0) {
                totalFixed += isRow ? this.childSizes.get(i).width() : this.childSizes.get(i).height();
            } else {
                totalWeight += w;
            }
        }
        float available = (isRow ? width : height) - totalFixed - this.spacing * (this.children.size() - 1);

        float currentMain = isRow ? x : y;
        List<LayoutRect> rects = new ArrayList<>(this.children.size());
        for (int i = 0; i < this.children.size(); i++) {
            UIComponent child = this.children.get(i);
            MeasuredSize size = this.childSizes.get(i);
            float w = i < this.childFlexGrows.size() ? this.childFlexGrows.get(i) : 1f;
            float childMain;
            if (w <= 0 || totalWeight <= 0) {
                childMain = isRow ? size.width() : size.height();
            } else {
                childMain = (w / totalWeight) * Math.max(0, available);
            }
            LayoutRect rect;
            if (isRow) {
                float childY = y + this.crossAlignV.align(height, size.height());
                rect = LayoutRect.of(currentMain, childY, childMain, size.height());
                LayoutHelper.layoutChild(child, currentMain, childY, childMain, size.height());
                currentMain += childMain + this.spacing;
            } else {
                float childX = x + this.crossAlignH.align(width, size.width());
                rect = LayoutRect.of(childX, currentMain, size.width(), childMain);
                LayoutHelper.layoutChild(child, childX, currentMain, size.width(), childMain);
                currentMain += childMain + this.spacing;
            }
            rects.add(rect);
        }
        this.childRects = rects;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        LayoutHelper.renderChildrenSorted(this.children, this.childRects, extractor);
    }
}
