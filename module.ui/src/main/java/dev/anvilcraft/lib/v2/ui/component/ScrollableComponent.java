package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可滚动容器。限制最大高度，超出部分可垂直滚动。
 * 渲染时自动裁剪，并绘制滚动条。
 */
public class ScrollableComponent implements UIComponent {

    private static final int SCROLLBAR_COLOR = 0xFF888888;
    private static final int SCROLLBAR_BG    = 0xFF333333;
    private static final int SCROLLBAR_W     = 4;

    private Modifier modifier;
    private final float maxHeight;
    private List<UIComponent> children = Collections.emptyList();
    private List<MeasuredSize> childSizes = Collections.emptyList();

    private float scrollY;
    private float contentHeight;

    private float x, y, width, height;

    public ScrollableComponent(Modifier modifier, float maxHeight) {
        this.modifier = modifier;
        this.maxHeight = maxHeight;
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }

    public ScrollableComponent modifier(Modifier m) { this.modifier = m; return this; }

    @Override public Modifier modifier() { return modifier; }
    @Override public List<UIComponent> children() { return children; }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        if (children.isEmpty()) return MeasuredSize.ZERO;

        float maxW = 0;
        float totalH = 0;
        List<MeasuredSize> sizes = new ArrayList<>(children.size());
        Constraints childC = new Constraints(0, constraints.maxWidth(), 0, Float.MAX_VALUE);

        for (UIComponent child : children) {
            MeasuredSize s = child.measure(childC);
            // 修饰符会扩展尺寸（如 padding），需要计入内容高度
            s = child.modifier().foldOut(s,
                    (el, sz) -> el.modifyMeasuredSize(child, childC, sz));
            sizes.add(s);
            totalH += s.height();
            maxW = Math.max(maxW, s.width());
        }
        this.childSizes = sizes;
        this.contentHeight = totalH;

        return MeasuredSize.of(
                constraints.constrainWidth(maxW),
                constraints.constrainHeight(Math.min(totalH, maxHeight))
        );
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        float currentY = y + scrollY;
        for (int i = 0; i < children.size(); i++) {
            UIComponent child = children.get(i);
            MeasuredSize size = childSizes.get(i);
            child.layout(x, currentY, width, size.height());
            currentY += size.height();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        int ix = (int) x, iy = (int) y, iw = (int) width, ih = (int) height;

        // 裁剪到容器范围
        extractor.enableScissor(ix, iy, ix + iw, iy + ih);

        for (UIComponent child : children) {
            child.extractRenderState(extractor);
        }

        extractor.disableScissor();

        // 滚动条
        if (contentHeight > height) {
            float barH = Math.max(16, height * height / contentHeight);
            float maxScroll = contentHeight - height;
            float barY = y + (-scrollY / maxScroll) * (height - barH);
            int bx = (int) (x + width - SCROLLBAR_W - 1);
            extractor.fill(bx, iy, bx + SCROLLBAR_W, iy + ih, SCROLLBAR_BG);
            extractor.fill(bx, (int) barY, bx + SCROLLBAR_W, (int) (barY + barH), SCROLLBAR_COLOR);
        }
    }

    // ── 滚动 ──

    /** 处理滚轮事件。返回 true 表示已消费。 */
    public boolean onScroll(float amount) {
        if (contentHeight <= height) return false;
        float maxScroll = contentHeight - height;
        scrollY = Mth.clamp(scrollY + amount * 20, -maxScroll, 0);
        return true;
    }

    public float getScrollY() { return scrollY; }
    public void setScrollY(float scrollY) { this.scrollY = scrollY; }

    /** 命中测试包围盒。 */
    public LayoutRect hitRect() {
        return LayoutRect.of(x, y, width, height);
    }
}
