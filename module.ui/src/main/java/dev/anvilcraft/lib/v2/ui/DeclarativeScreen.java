package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.component.ButtonComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

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
    private int lastMouseX, lastMouseY;

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
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (composition != null) {
            composition.renderFrame(extractor, this.width, this.height);
            updateHover(mouseX, mouseY);
        }
    }

    // ── 鼠标输入 ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            for (UIComponent child : rootScope.getChildren()) {
                if (hitTestClick(child, lastMouseX, lastMouseY)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 后续 Phase: 路由到 ScrollComponent
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ── 键盘输入 ──

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { // ESC — 关闭 Screen
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    // ── 命中测试 ──

    /** 命中测试 + 点击触发。子组件优先（后绘制在上层）。 */
    private boolean hitTestClick(UIComponent component, float px, float py) {
        var children = component.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (hitTestClick(children.get(i), px, py)) return true;
        }
        if (component instanceof ButtonComponent btn && btn.hitRect().contains(px, py)) {
            btn.click();
            return true;
        }
        return false;
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

