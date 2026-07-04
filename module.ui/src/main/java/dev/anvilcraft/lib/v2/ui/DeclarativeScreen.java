package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.component.DropdownComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * 声明式 UI 的 {@link Screen} 宿主。
 * <p>
 * 每帧自动完成：dirty check → recompose → measure → layout → render states。
 * 输入事件通过递归遍历组件树分发，各组件覆写 {@link UIComponent} 的事件方法处理自身逻辑。
 */
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public abstract class DeclarativeScreen extends Screen {
    private final UIScope rootScope = new RootScope();
    @Nullable
    private Composition composition;
    @Nullable
    private UIComponent focusOwner;

    protected DeclarativeScreen(Component title) {
        super(title);
    }

    protected abstract void content(@Nullable UIScope scope);

    // ── 每帧渲染 ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        if (this.composition != null) {
            this.composition.renderFrame(extractor, this.width, this.height);
            this.updateHover(mouseX, mouseY);
            this.refreshFocus();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.onClose();
            return true;
        }
        if (this.focusOwner != null && this.focusOwner.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    // ── 焦点 ──

    @Override
    protected void init() {
        if (this.composition == null) {
            this.composition = new Composition(this.rootScope);
            this.composition.setContent(this::content);
        }
    }

    /**
     * 窗口 resize 时不重建组件树，只标记 dirty 用新尺寸 recompose。
     */
    @Override
    protected void repositionElements() {
        if (this.composition != null) this.composition.invalidate();
    }

    private void refreshFocus() {
        if (this.focusOwner == null) return;
        for (UIComponent child : this.rootScope.getChildren()) {
            UIComponent found = findFocused(child);
            if (found != null) {
                this.focusOwner = found;
                return;
            }
        }
        this.focusOwner = null;
    }

    // ── 鼠标点击 ──

    private @Nullable UIComponent findFocused(UIComponent component) {
        if (component instanceof Focusable f && f.focused()) return component;
        for (UIComponent child : component.children()) {
            UIComponent found = findFocused(child);
            if (found != null) return found;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, isDoubleClick);

        this.clearAllFocus();
        this.closeAllDropdowns();

        if (this.dispatchMouseClicked(event, isDoubleClick)) return true;

        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        List<UIComponent> sorted = this.rootScope.getChildren().stream()
            .sorted(Comparator.comparingInt(UIComponent::eventPriority).reversed())
            .toList();
        for (UIComponent child : sorted) {
            dispatchMouseReleased(child, event);
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        List<UIComponent> sorted = this.rootScope.getChildren().stream()
            .sorted(Comparator.comparingInt(UIComponent::eventPriority).reversed())
            .toList();
        for (UIComponent child : sorted) {
            if (dispatchMouseDragged(child, event, deltaX, deltaY)) return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<UIComponent> sorted = this.rootScope.getChildren().stream()
            .sorted(Comparator.comparingInt(UIComponent::eventPriority).reversed())
            .toList();
        for (UIComponent child : sorted) {
            if (dispatchMouseScrolled(child, mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.focusOwner != null && this.focusOwner.charTyped(event)) return true;
        return super.charTyped(event);
    }

    private void clearAllFocus() {
        this.focusOwner = null;
        for (UIComponent child : this.rootScope.getChildren()) clearFocusRecursive(child);
    }

    // ── 鼠标拖拽 ──

    private void clearFocusRecursive(UIComponent component) {
        if (component instanceof Focusable f) f.setFocused(false);
        for (UIComponent child : component.children()) clearFocusRecursive(child);
    }

    private void closeAllDropdowns() {
        for (UIComponent child : this.rootScope.getChildren()) closeDropdownsRecursive(child);
    }

    // ── 鼠标释放 ──

    private void closeDropdownsRecursive(UIComponent component) {
        if (component instanceof DropdownComponent dd) dd.setOpen(false);
        for (UIComponent child : component.children()) closeDropdownsRecursive(child);
    }

    private boolean dispatchMouseClicked(MouseButtonEvent event, boolean isDouble) {
        List<UIComponent> sorted = this.rootScope.getChildren().stream()
            .sorted(Comparator.comparingInt(UIComponent::eventPriority).reversed())
            .toList();
        for (UIComponent child : sorted) {
            if (dispatchMouseClickedRecursive(child, event, isDouble)) return true;
        }
        return false;
    }

    // ── 滚轮 ──

    private boolean dispatchMouseClickedRecursive(UIComponent component, MouseButtonEvent event, boolean isDouble) {
        var sorted = component.children()
            .stream()
            .sorted(java.util.Comparator.comparingInt(UIComponent::eventPriority).reversed())
            .toList();
        for (UIComponent child : sorted) {
            if (dispatchMouseClickedRecursive(child, event, isDouble)) return true;
        }
        if (component.mouseClicked(event, isDouble)) {
            if (component instanceof Focusable) this.focusOwner = component;
            return true;
        }
        return false;
    }

    private boolean dispatchMouseDragged(UIComponent component, MouseButtonEvent event, double dx, double dy) {
        var sorted = component.children()
            .stream()
            .sorted(java.util.Comparator.comparingInt(UIComponent::eventPriority).reversed())
            .toList();
        for (UIComponent child : sorted) {
            if (dispatchMouseDragged(child, event, dx, dy)) return true;
        }
        return component.mouseDragged(event, dx, dy);
    }

    // ── 键盘 ──

    private void dispatchMouseReleased(UIComponent component, MouseButtonEvent event) {
        var sorted = component.children()
            .stream()
            .sorted(java.util.Comparator.comparingInt(UIComponent::eventPriority).reversed())
            .toList();
        for (UIComponent child : sorted) dispatchMouseReleased(child, event);
        component.mouseReleased(event);
    }

    private boolean dispatchMouseScrolled(UIComponent component, double mx, double my, double sx, double sy) {
        var sorted = component.children()
            .stream()
            .sorted(java.util.Comparator.comparingInt(UIComponent::eventPriority).reversed())
            .toList();
        for (UIComponent child : sorted) {
            if (dispatchMouseScrolled(child, mx, my, sx, sy)) return true;
        }
        return component.mouseScrolled(mx, my, sx, sy);
    }

    // ── hover ──

    private void updateHover(float mouseX, float mouseY) {
        for (UIComponent child : this.rootScope.getChildren()) {
            updateHoverRecursive(child, mouseX, mouseY);
        }
    }

    private void updateHoverRecursive(UIComponent component, float mx, float my) {
        component.updateHover(mx, my);
        for (UIComponent child : component.children()) updateHoverRecursive(child, mx, my);
    }

    // ── 内部类 ──

    private static class RootScope extends UIScope {
    }
}
