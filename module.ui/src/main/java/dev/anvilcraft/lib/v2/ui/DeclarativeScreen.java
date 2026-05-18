package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.component.ButtonComponent;
import dev.anvilcraft.lib.v2.ui.component.CheckboxComponent;
import dev.anvilcraft.lib.v2.ui.component.ScrollableComponent;
import dev.anvilcraft.lib.v2.ui.component.SliderComponent;
import dev.anvilcraft.lib.v2.ui.component.TextFieldComponent;
import dev.anvilcraft.lib.v2.ui.input.KeyInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
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
public abstract class DeclarativeScreen extends Screen {

    @Nullable
    private Composition composition;
    private final UIScope rootScope = new RootScope();
    @Nullable
    private KeyInputHandler focusOwner;

    protected DeclarativeScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        composition = new Composition(rootScope);
        composition.setContent(this::content);
    }

    /** 声明 UI 内容。初始组合和每次 recompose 时调用。 */
    protected abstract void content(UIScope scope);

    // ── 每帧渲染 ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        if (composition != null) {
            composition.renderFrame(extractor, this.width, this.height);
            updateHover(mouseX, mouseY);
        }
    }

    // ── 鼠标输入 ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            var mc = Minecraft.getInstance();
            int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
            int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
            // 点击空白处清除焦点
            focusOwner = null;
            for (UIComponent child : rootScope.getChildren()) {
                clearFocusRecursive(child);
            }
            for (UIComponent child : rootScope.getChildren()) {
                if (hitTestClick(child, mx, my)) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        for (UIComponent child : rootScope.getChildren()) {
            if (hitTestDrag(child, mx, my)) return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (UIComponent child : rootScope.getChildren()) {
            if (hitTestScroll(child, (float) mouseX, (float) mouseY, (float) scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ── 键盘输入 ──

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            onClose();
            return true;
        }
        if (focusOwner != null && focusOwner.onKeyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    // ── 命中测试 ──

    /** 命中测试 + 点击触发。子组件优先。 */
    private boolean hitTestClick(UIComponent component, float px, float py) {
        var children = component.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (hitTestClick(children.get(i), px, py)) return true;
        }
        if (component instanceof ButtonComponent btn && btn.hitRect().contains(px, py)) {
            btn.click();
            return true;
        }
        if (component instanceof CheckboxComponent cb && cb.hitRect().contains(px, py)) {
            cb.toggle();
            return true;
        }
        if (component instanceof SliderComponent sl && sl.hitRect().contains(px, py)) {
            sl.setValueFromMouse(px);
            return true;
        }
        if (component instanceof TextFieldComponent tf && tf.hitRect().contains(px, py)) {
            tf.setFocused(true);
            focusOwner = tf;
            return true;
        }
        return false;
    }

    /** 拖拽命中测试（仅 Slider 响应）。 */
    private boolean hitTestDrag(UIComponent component, float px, float py) {
        var children = component.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (hitTestDrag(children.get(i), px, py)) return true;
        }
        if (component instanceof SliderComponent sl && sl.hitRect().contains(px, py)) {
            sl.setValueFromMouse(px);
            return true;
        }
        return false;
    }

    /** 滚轮命中测试（仅 ScrollableComponent 响应）。 */
    private boolean hitTestScroll(UIComponent component, float px, float py, float amount) {
        var children = component.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (hitTestScroll(children.get(i), px, py, amount)) return true;
        }
        if (component instanceof ScrollableComponent sc && sc.hitRect().contains(px, py)) {
            return sc.onScroll(amount);
        }
        return false;
    }

    /** 清除组件树中所有 TextField 的焦点。 */
    private void clearFocusRecursive(UIComponent component) {
        if (component instanceof TextFieldComponent tf) tf.setFocused(false);
        for (UIComponent child : component.children()) clearFocusRecursive(child);
    }

    /** 遍历组件树，更新 ButtonComponent 的 hover 状态。 */
    private void updateHover(float mouseX, float mouseY) {
        for (UIComponent child : rootScope.getChildren()) {
            updateHoverRecursive(child, mouseX, mouseY);
        }
    }

    private void updateHoverRecursive(UIComponent component, float mx, float my) {
        if (component instanceof ButtonComponent btn) {
            btn.setHovered(btn.hitRect().contains(mx, my));
        }
        for (UIComponent child : component.children()) {
            updateHoverRecursive(child, mx, my);
        }
    }

    // ── internal ──

    private static class RootScope extends UIScope {
    }
}

