package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.component.ButtonComponent;
import dev.anvilcraft.lib.v2.ui.component.DropdownComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

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
    protected void init() {
        this.composition = new Composition(this.rootScope);
        this.composition.setContent(this::content);
    }

    // ── 焦点 ──

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

    private @Nullable UIComponent findFocused(UIComponent component) {
        if (component instanceof Focusable f && f.focused()) return component;
        for (UIComponent child : component.children()) {
            UIComponent found = findFocused(child);
            if (found != null) return found;
        }
        return null;
    }

    // ── 鼠标点击 ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, isDoubleClick);

        this.clearAllFocus();

        if (this.dispatchMouseClicked(event, isDoubleClick)) return true;

        this.closeAllDropdowns();
        return super.mouseClicked(event, isDoubleClick);
    }

    private void clearAllFocus() {
        this.focusOwner = null;
        for (UIComponent child : this.rootScope.getChildren()) clearFocusRecursive(child);
    }

    private void clearFocusRecursive(UIComponent component) {
        if (component instanceof Focusable f) f.setFocused(false);
        for (UIComponent child : component.children()) clearFocusRecursive(child);
    }

    private void closeAllDropdowns() {
        for (UIComponent child : this.rootScope.getChildren()) closeDropdownsRecursive(child);
    }

    private void closeDropdownsRecursive(UIComponent component) {
        if (component instanceof DropdownComponent dd) dd.setOpen(false);
        for (UIComponent child : component.children()) closeDropdownsRecursive(child);
    }

    private boolean dispatchMouseClicked(MouseButtonEvent event, boolean isDouble) {
        for (UIComponent child : this.rootScope.getChildren()) {
            if (dispatchMouseClickedRecursive(child, event, isDouble)) return true;
        }
        return false;
    }

    private boolean dispatchMouseClickedRecursive(UIComponent component, MouseButtonEvent event, boolean isDouble) {
        var sorted = component.children().stream()
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

    // ── 鼠标拖拽 ──

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        for (UIComponent child : this.rootScope.getChildren()) {
            if (dispatchMouseDragged(child, event, deltaX, deltaY)) return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    private boolean dispatchMouseDragged(UIComponent component, MouseButtonEvent event, double dx, double dy) {
        var sorted = component.children().stream()
                .sorted(java.util.Comparator.comparingInt(UIComponent::eventPriority).reversed())
                .toList();
        for (UIComponent child : sorted) {
            if (dispatchMouseDragged(child, event, dx, dy)) return true;
        }
        return component.mouseDragged(event, dx, dy);
    }

    // ── 鼠标释放 ──

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (UIComponent child : this.rootScope.getChildren()) {
            dispatchMouseReleased(child, event);
        }
        return super.mouseReleased(event);
    }

    private void dispatchMouseReleased(UIComponent component, MouseButtonEvent event) {
        var sorted = component.children().stream()
                .sorted(java.util.Comparator.comparingInt(UIComponent::eventPriority).reversed())
                .toList();
        for (UIComponent child : sorted) dispatchMouseReleased(child, event);
        component.mouseReleased(event);
    }

    // ── 滚轮 ──

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (UIComponent child : this.rootScope.getChildren()) {
            if (dispatchMouseScrolled(child, mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean dispatchMouseScrolled(UIComponent component, double mx, double my, double sx, double sy) {
        var sorted = component.children().stream()
                .sorted(java.util.Comparator.comparingInt(UIComponent::eventPriority).reversed())
                .toList();
        for (UIComponent child : sorted) {
            if (dispatchMouseScrolled(child, mx, my, sx, sy)) return true;
        }
        return component.mouseScrolled(mx, my, sx, sy);
    }

    // ── 键盘 ──

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.onClose();
            return true;
        }
        if (this.focusOwner != null && this.focusOwner.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.focusOwner != null && this.focusOwner.charTyped(event)) return true;
        return super.charTyped(event);
    }

    // ── hover ──

    private void updateHover(float mouseX, float mouseY) {
        for (UIComponent child : this.rootScope.getChildren()) {
            updateHoverRecursive(child, mouseX, mouseY);
        }
    }

    private void updateHoverRecursive(UIComponent component, float mx, float my) {
        if (component instanceof ButtonComponent btn) btn.setHovered(btn.hitRect().contains(mx, my));
        for (UIComponent child : component.children()) updateHoverRecursive(child, mx, my);
    }

    // ── 内部类 ──

    private static class RootScope extends UIScope {
    }
}
