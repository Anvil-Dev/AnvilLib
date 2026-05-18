package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.component.ButtonComponent;
import dev.anvilcraft.lib.v2.ui.component.CheckboxComponent;
import dev.anvilcraft.lib.v2.ui.component.DropdownComponent;
import dev.anvilcraft.lib.v2.ui.component.ScrollableComponent;
import dev.anvilcraft.lib.v2.ui.component.SliderComponent;
import dev.anvilcraft.lib.v2.ui.component.TextInputComponent;
import dev.anvilcraft.lib.v2.ui.input.KeyInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import org.jspecify.annotations.Nullable;

/**
 * 声明式 UI 的 {@link Screen} 宿主。
 * <p>
 * 每帧自动完成：dirty check → recompose → measure → layout → render states。
 * 输入事件（点击、按键、滚轮）通过命中测试路由到对应组件。
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
    private KeyInputHandler focusOwner;

    protected DeclarativeScreen(Component title) {
        super(title);
    }

    /**
     * 声明 UI 内容。初始组合和每次 recompose 时调用。
     */
    protected abstract void content(@Nullable UIScope scope);

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        if (this.composition != null) {
            this.composition.renderFrame(extractor, this.width, this.height);
            this.updateHover(mouseX, mouseY);
            this.refreshFocus();
            for (UIComponent child : this.rootScope.getChildren()) {
                this.renderPopups(child, extractor);
            }
        }
    }

    // ── 每帧渲染 ──

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.onClose();
            return true;
        }
        if (this.focusOwner != null && this.focusOwner.onKeyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    protected void init() {
        this.composition = new Composition(this.rootScope);
        this.composition.setContent(this::content);
    }

    private void renderPopups(UIComponent component, GuiGraphicsExtractor extractor) {
        if (component instanceof DropdownComponent dd) dd.renderPopup(extractor);
        for (UIComponent child : component.children()) this.renderPopups(child, extractor);
    }

    // ── 鼠标输入 ──

    /**
     * recompose 后重新绑定 focusOwner（旧实例可能已被替换）。
     */
    private void refreshFocus() {
        if (this.focusOwner == null) return;
        for (UIComponent child : this.rootScope.getChildren()) {
            KeyInputHandler found = this.findFocused(child);
            if (found != null) {
                this.focusOwner = found;
                return;
            }
        }
        this.focusOwner = null;
    }

    private @Nullable KeyInputHandler findFocused(UIComponent component) {
        if (component instanceof TextInputComponent tf && tf.focused()) return tf;
        for (UIComponent child : component.children()) {
            KeyInputHandler found = this.findFocused(child);
            if (found != null) return found;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            var mc = Minecraft.getInstance();
            int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
            int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());

            this.focusOwner = null;
            for (UIComponent child : this.rootScope.getChildren()) {
                this.clearFocusRecursive(child);
            }

            boolean hit = false;
            for (UIComponent child : this.rootScope.getChildren()) {
                if (this.hitTestClick(child, mx, my)) {
                    hit = true;
                    break;
                }
            }

            if (!hit) {
                for (UIComponent child : this.rootScope.getChildren()) {
                    this.closeDropdownsRecursive(child);
                }
            }

            if (hit) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (UIComponent child : this.rootScope.getChildren()) {
            this.stopDragRecursive(child);
        }
        return super.mouseReleased(event);
    }

    // ── 键盘输入 ──

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        for (UIComponent child : this.rootScope.getChildren()) {
            if (this.hitTestDrag(child, mx, my)) return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (UIComponent child : this.rootScope.getChildren()) {
            if (this.hitTestScroll(child, (float) mouseX, (float) mouseY, (float) scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.focusOwner != null && this.focusOwner.onCharTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    // ── 命中测试 ──

    /**
     * 命中测试 + 点击触发。子组件优先。
     */
    private boolean hitTestClick(UIComponent component, float px, float py) {
        var children = component.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (this.hitTestClick(children.get(i), px, py)) return true;
        }
        switch (component) {
            case ButtonComponent btn when btn.hitRect().contains(px, py) -> {
                btn.click();
                return true;
            }
            case CheckboxComponent cb when cb.hitRect().contains(px, py) -> {
                cb.toggle();
                return true;
            }
            case SliderComponent sl when sl.hitRect().contains(px, py) -> {
                sl.setValueFromMouse(px);
                return true;
            }
            case TextInputComponent tf when tf.hitRect().contains(px, py) -> {
                tf.setFocused(true);
                this.focusOwner = tf;
                return true;
            }
            case DropdownComponent dd -> {
                if (dd.isOnPopupScrollbar(px, py)) {
                    dd.startPopupScrollbarDrag(py);
                    return true;
                }
                if (dd.clickPopup(px, py)) return true;
                return dd.clickTrigger(px, py);
            }
            case ScrollableComponent sc when sc.isOnScrollbar(px, py) -> {
                sc.startScrollbarDrag(py);
                return true;
            }
            default -> {
            }
        }
        return false;
    }

    /**
     * 拖拽命中测试（Slider + Scrollable 滚动条）。
     */
    private boolean hitTestDrag(UIComponent component, float px, float py) {
        var children = component.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (this.hitTestDrag(children.get(i), px, py)) return true;
        }
        switch (component) {
            case SliderComponent sl when sl.hitRect().contains(px, py) -> {
                sl.setValueFromMouse(px);
                return true;
            }
            case ScrollableComponent sc when sc.scrollbarDragging() -> {
                sc.onScrollbarDrag(py);
                return true;
            }
            case DropdownComponent dd when dd.scrollbarDragging() -> {
                dd.onPopupScrollbarDrag(py);
                return true;
            }
            default -> {
            }
        }
        return false;
    }

    /**
     * 递归关闭所有 Dropdown。
     */
    private void closeDropdownsRecursive(UIComponent component) {
        if (component instanceof DropdownComponent dd) dd.setOpen(false);
        for (UIComponent child : component.children()) this.closeDropdownsRecursive(child);
    }

    /**
     * 递归停止拖拽状态。
     */
    private void stopDragRecursive(UIComponent component) {
        if (component instanceof ScrollableComponent sc) sc.stopScrollbarDrag();
        if (component instanceof DropdownComponent dd) dd.stopPopupScrollbarDrag();
        for (UIComponent child : component.children()) this.stopDragRecursive(child);
    }

    /**
     * 滚轮命中测试（ScrollableComponent + Dropdown 弹出层）。
     */
    private boolean hitTestScroll(UIComponent component, float px, float py, float amount) {
        var children = component.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (this.hitTestScroll(children.get(i), px, py, amount)) return true;
        }
        if (component instanceof ScrollableComponent sc && sc.hitRect().contains(px, py)) {
            return sc.onScroll(amount);
        }
        if (component instanceof DropdownComponent dd && dd.open()
            && dd.popupRect().contains(px, py)) {
            return dd.onPopupScroll(amount);
        }
        return false;
    }

    /**
     * 清除组件树中所有 TextField 的焦点。
     */
    private void clearFocusRecursive(UIComponent component) {
        if (component instanceof TextInputComponent tf) tf.setFocused(false);
        for (UIComponent child : component.children()) this.clearFocusRecursive(child);
    }

    /**
     * 遍历组件树，更新 ButtonComponent 的 hover 状态。
     */
    private void updateHover(float mouseX, float mouseY) {
        for (UIComponent child : this.rootScope.getChildren()) {
            this.updateHoverRecursive(child, mouseX, mouseY);
        }
    }

    private void updateHoverRecursive(UIComponent component, float mx, float my) {
        if (component instanceof ButtonComponent btn) {
            btn.setHovered(btn.hitRect().contains(mx, my));
        }
        for (UIComponent child : component.children()) {
            this.updateHoverRecursive(child, mx, my);
        }
    }

    // ── 内部类 ──

    private static class RootScope extends UIScope {
    }
}

