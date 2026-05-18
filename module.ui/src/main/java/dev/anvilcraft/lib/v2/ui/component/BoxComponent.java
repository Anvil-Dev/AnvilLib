package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Alignment;
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
 * 层叠布局。所有子组件重叠于同一区域，按声明顺序从底到顶绘制。
 * Box 本身的大小由最大的子组件决定。
 */
@Accessors(fluent = true)
public class BoxComponent implements UIComponent {

    @Getter
    @Setter
    private Modifier modifier;
    @Getter
    private List<UIComponent> children = Collections.emptyList();
    private List<MeasuredSize> childSizes = Collections.emptyList();

    private Alignment.Horizontal contentAlignmentH = Alignment.Horizontal.Start;
    private Alignment.Vertical contentAlignmentV = Alignment.Vertical.Top;

    private float x, y, width, height;

    public BoxComponent(Modifier modifier) {
        this.modifier = modifier;
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }

    public BoxComponent contentAlignment(Alignment.Horizontal h, Alignment.Vertical v) {
        this.contentAlignmentH = h;
        this.contentAlignmentV = v;
        return this;
    }


    @Override
    public MeasuredSize measure(Constraints constraints) {
        if (children.isEmpty()) return MeasuredSize.ZERO;

        float maxWidth = 0;
        float maxHeight = 0;
        List<MeasuredSize> sizes = new ArrayList<>(children.size());

        for (UIComponent child : children) {
            MeasuredSize size = child.measure(constraints);
            sizes.add(size);
            maxWidth = Math.max(maxWidth, size.width());
            maxHeight = Math.max(maxHeight, size.height());
        }

        this.childSizes = sizes;
        return MeasuredSize.of(
            constraints.constrainWidth(maxWidth),
            constraints.constrainHeight(maxHeight)
        );
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            MeasuredSize size = childSizes.get(i);
            float childX = x + contentAlignmentH.align(width, size.width());
            float childY = y + contentAlignmentV.align(height, size.height());
            child.layout(childX, childY, size.width(), size.height());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        for (UIComponent child : children) {
            child.extractRenderState(extractor);
        }
    }
}
