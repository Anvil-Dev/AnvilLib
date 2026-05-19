package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 虚拟化纵向列表。仅渲染可见区域的项，支持滚动。
 * 每项固定高度，通过 {@code itemHeight} 指定。
 */
@Accessors(fluent = true)
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class LazyColumnComponent implements UIComponent {

    private static final int SCROLLBAR_COLOR = 0xFF888888;
    private static final int SCROLLBAR_BG = 0xFF333333;
    private static final int SCROLLBAR_W = 4;

    @Getter @Setter
    private Modifier modifier;
    private final float itemHeight;
    private final float maxHeight;
    private final List<?> items;
    private final ItemBuilder<?> builder;
    private List<UIComponent> children = Collections.emptyList();
    private float scrollOffset;

    @Getter
    private boolean scrollbarDragging;
    private float dragAnchorY;

    private float x, y, width, height;

    /** 列表项构建函数接口。 */
    @FunctionalInterface
    public interface ItemBuilder<T> {
        UIComponent build(T item);
    }

    public <T> LazyColumnComponent(Modifier modifier, float itemHeight, float maxHeight,
                                    List<T> items, ItemBuilder<T> builder) {
        this.modifier = modifier;
        this.itemHeight = itemHeight;
        this.maxHeight = maxHeight;
        this.items = List.copyOf(items);
        this.builder = builder;
    }

    @Override
    public List<UIComponent> children() { return this.children; }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        float contentH = this.items.size() * this.itemHeight;
        float displayH = Math.min(contentH, this.maxHeight);
        return MeasuredSize.of(constraints.constrainWidth(200), constraints.constrainHeight(displayH));
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        rebuildChildren();
        float maxScroll = Math.max(0, this.items.size() * this.itemHeight - height);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void rebuildChildren() {
        int first = (int) (this.scrollOffset / this.itemHeight);
        int visible = Math.min((int) (this.height / this.itemHeight) + 1, this.items.size() - first);
        List<UIComponent> newChildren = new ArrayList<>(visible);
        for (int i = 0; i < visible; i++) {
            Object item = this.items.get(first + i);
            newChildren.add(((ItemBuilder) this.builder).build(item));
        }
        this.children = newChildren;
        // Position each child
        for (int i = 0; i < visible; i++) {
            UIComponent child = newChildren.get(i);
            float childY = this.y - (this.scrollOffset % this.itemHeight) + i * this.itemHeight;
            child.measure(Constraints.NONE);
            child.layout(this.x, childY, this.width, this.itemHeight);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        int ix = (int) this.x, iy = (int) this.y, iw = (int) this.width, ih = (int) this.height;
        extractor.enableScissor(ix, iy, ix + iw, iy + ih);

        for (UIComponent child : this.children) {
            child.extractRenderState(extractor);
        }

        extractor.disableScissor();

        // 滚动条
        float contentH = this.items.size() * this.itemHeight;
        if (contentH > this.height) {
            float bh = Math.max(16, this.height * this.height / contentH);
            float maxScroll = contentH - this.height;
            float by = this.y + (this.scrollOffset / maxScroll) * (this.height - bh);
            int bx = (int) (this.x + this.width - SCROLLBAR_W - 1);
            extractor.fill(bx, iy, bx + SCROLLBAR_W, iy + ih, SCROLLBAR_BG);
            extractor.fill(bx, (int) by, bx + SCROLLBAR_W, (int) (by + bh), SCROLLBAR_COLOR);
        }
    }

    // ── 滚动 ──

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.hitRect().contains((float) mouseX, (float) mouseY)) return false;
        this.scrollOffset = Mth.clamp(
                this.scrollOffset + (float) (-scrollY * 20),
                0, Math.max(0, this.items.size() * this.itemHeight - this.height)
        );
        return true;
    }

    public boolean isOnScrollbar(float mx, float my) {
        float contentH = this.items.size() * this.itemHeight;
        if (contentH <= this.height) return false;
        float bh = Math.max(16, this.height * this.height / contentH);
        float maxScroll = contentH - this.height;
        float by = this.y + (this.scrollOffset / maxScroll) * (this.height - bh);
        int bx = (int) (this.x + this.width - SCROLLBAR_W - 1);
        return mx >= bx && mx < bx + SCROLLBAR_W && my >= by && my < by + bh;
    }

    public void startScrollbarDrag(float my) {
        this.scrollbarDragging = true;
        float contentH = this.items.size() * this.itemHeight;
        float bh = Math.max(16, this.height * this.height / contentH);
        float maxScroll = contentH - this.height;
        float by = this.y + (this.scrollOffset / maxScroll) * (this.height - bh);
        this.dragAnchorY = my - by;
    }

    public void onScrollbarDrag(float my) {
        if (!this.scrollbarDragging) return;
        float contentH = this.items.size() * this.itemHeight;
        float bh = Math.max(16, this.height * this.height / contentH);
        float maxScroll = contentH - this.height;
        float newBarY = my - this.dragAnchorY;
        float ratio = Mth.clamp((newBarY - this.y) / (this.height - bh), 0f, 1f);
        this.scrollOffset = ratio * maxScroll;
    }

    public void stopScrollbarDrag() { this.scrollbarDragging = false; }

    public LayoutRect hitRect() {
        return LayoutRect.of(this.x, this.y, this.width, this.height);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDouble) {
        if (event.button() != 0) return false;
        var mc = net.minecraft.client.Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (this.isOnScrollbar(mx, my)) { this.startScrollbarDrag(my); return true; }
        for (UIComponent child : this.children) {
            if (child.mouseClicked(event, isDouble)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (!this.scrollbarDragging) return false;
        var mc = net.minecraft.client.Minecraft.getInstance();
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        this.onScrollbarDrag(my);
        return true;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        this.stopScrollbarDrag();
        return false;
    }
}
