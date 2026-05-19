package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可滚动容器。限制最大高度，超出部分可垂直滚动。
 * 渲染时自动裁剪，并绘制滚动条。
 */
@Accessors(fluent = true)
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class ScrollableComponent implements UIComponent {
    private static final int SCROLLBAR_COLOR = 0xFF888888;
    private static final int SCROLLBAR_BG = 0xFF333333;
    private static final int SCROLLBAR_W = 4;
    private final float maxHeight;
    @Getter
    @Setter
    private Modifier modifier;
    @Getter
    private List<UIComponent> children = Collections.emptyList();
    private List<MeasuredSize> childSizes = Collections.emptyList();

    @Getter
    private float scrollY;
    private float contentHeight;
    @Getter
    private boolean scrollbarDragging;
    private float dragAnchorY;

    @Getter
    private float x, y, width, height;

    public ScrollableComponent(Modifier modifier, float maxHeight) {
        this.modifier = modifier;
        this.maxHeight = maxHeight;
    }

    public void setChildren(List<UIComponent> children) {
        this.children = List.copyOf(children);
    }


    public MeasuredSize measure(Constraints constraints) {
        if (this.children.isEmpty()) return MeasuredSize.ZERO;

        float maxW = 0;
        float totalH = 0;
        List<MeasuredSize> sizes = new ArrayList<>(this.children.size());
        Constraints childC = new Constraints(0, constraints.maxWidth() - ScrollableComponent.SCROLLBAR_W - 1, 0, Float.MAX_VALUE);

        for (UIComponent child : this.children) {
            MeasuredSize s = child.measure(childC);
            // 修饰符会扩展尺寸（如 padding），需要计入内容高度
            s = child.modifier().foldOut(s, (el, sz) -> el.modifyMeasuredSize(child, childC, sz));
            sizes.add(s);
            totalH += s.height();
            maxW = Math.max(maxW, s.width());
        }
        this.childSizes = sizes;
        this.contentHeight = totalH;

        return MeasuredSize.of(constraints.constrainWidth(maxW), constraints.constrainHeight(Math.min(totalH, this.maxHeight)));
    }

    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        float currentY = y + this.scrollY;
        float childW = width - ScrollableComponent.SCROLLBAR_W - 1;
        for (int i = 0; i < this.children.size(); i++) {
            UIComponent child = this.children.get(i);
            MeasuredSize size = this.childSizes.get(i);
            child.layout(x, currentY, childW, size.height());
            currentY += size.height();
        }
    }

    public void extractRenderState(GuiGraphicsExtractor extractor) {
        int ix = (int) this.x, iy = (int) this.y, iw = (int) this.width, ih = (int) this.height;

        // 裁剪到容器范围
        extractor.enableScissor(ix, iy, ix + iw, iy + ih);

        for (UIComponent child : this.sortedChildren()) {
            child.extractRenderState(extractor);
        }

        extractor.disableScissor();

        // 滚动条
        if (this.contentHeight > this.height) {
            float bh = barH();
            float by = barY();
            int bx = (int) (this.x + this.width - ScrollableComponent.SCROLLBAR_W - 1);
            extractor.fill(bx, iy, bx + ScrollableComponent.SCROLLBAR_W, iy + ih, ScrollableComponent.SCROLLBAR_BG);
            extractor.fill(bx, (int) by, bx + ScrollableComponent.SCROLLBAR_W, (int) (by + bh), ScrollableComponent.SCROLLBAR_COLOR);
        }
    }

    // ── 滚动 ──

    public boolean onScroll(float amount) {
        if (this.contentHeight <= this.height) return false;
        float maxScroll = this.contentHeight - this.height;
        this.scrollY = Mth.clamp(this.scrollY + amount * 20, -maxScroll, 0);
        return true;
    }

    /**
     * 鼠标是否在滚动条滑块上。
     */
    public boolean isOnScrollbar(float mx, float my) {
        if (this.contentHeight <= this.height) return false;
        float bh = this.barH();
        float by = this.barY();
        int bx = (int) (this.x + this.width - ScrollableComponent.SCROLLBAR_W - 1);
        return mx >= bx && mx < bx + ScrollableComponent.SCROLLBAR_W && my >= by && my < by + bh;
    }

    /**
     * 开始拖拽滚动条。
     */
    public void startScrollbarDrag(float my) {
        this.scrollbarDragging = true;
        this.dragAnchorY = my - this.barY();
    }

    /**
     * 拖拽滚动条时更新位置。
     */
    public void onScrollbarDrag(float my) {
        if (!this.scrollbarDragging) return;
        float bh = this.barH();
        float maxScroll = this.contentHeight - this.height;
        float newBarY = my - this.dragAnchorY;
        float ratio = Mth.clamp(newBarY / (this.height - bh), 0f, 1f);
        this.scrollY = -(ratio * maxScroll);
    }

    /**
     * 停止拖拽。
     */
    public void stopScrollbarDrag() {
        this.scrollbarDragging = false;
    }


    private float barH() {
        return Math.max(16, this.height * this.height / this.contentHeight);
    }

    private float barY() {
        float maxScroll = this.contentHeight - this.height;
        return this.y + (-this.scrollY / maxScroll) * (this.height - this.barH());
    }

    public void setScrollY(float scrollY) {
        this.scrollY = scrollY;
    }

    /**
     * 命中测试包围盒。
     */
    public LayoutRect hitRect() {
        return LayoutRect.of(this.x, this.y, this.width, this.height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) return false;
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (this.isOnScrollbar(mx, my)) { this.startScrollbarDrag(my); return true; }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.hitRect().contains((float) mouseX, (float) mouseY)) {
            return this.onScroll((float) scrollY);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!this.scrollbarDragging()) return false;
        var mc = Minecraft.getInstance();
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        this.onScrollbarDrag(my);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.stopScrollbarDrag();
        return false;
    }
}
