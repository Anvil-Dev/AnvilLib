package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Alignment;
import dev.anvilcraft.lib.v2.ui.Arrangement;
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

/**
 * 横向线性布局。子组件自左而右排列。主轴=水平，交叉轴=垂直。
 */
@Accessors(fluent = true)
public class RowComponent implements UIComponent {

    @Getter
    @Setter
    private Modifier modifier;
    @Getter
    private List<UIComponent> children = Collections.emptyList();
    private List<MeasuredSize> childSizes = Collections.emptyList();

    @Setter
    private Arrangement.Horizontal horizontalArrangement = Arrangement.Horizontal.Start;
    @Setter
    private Alignment.Vertical verticalAlignment = Alignment.Vertical.Top;
    @Setter
    private float spacing;

    private float x, y, width, height;

    public RowComponent(Modifier modifier) {
        this.modifier = modifier;
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }


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

    public void extractRenderState(GuiGraphicsExtractor extractor) {
        for (UIComponent child : children) {
            child.extractRenderState(extractor);
        }
    }
}
