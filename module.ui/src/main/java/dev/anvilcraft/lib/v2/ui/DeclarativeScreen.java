package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.component.ButtonComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

/**
 * A {@link Screen} that hosts a declarative UI component tree.
 * <p>
 * Subclasses implement {@link #content(UIScope)} to declare the UI.
 * The composition is automatically managed each frame.
 *
 * <pre>{@code
 * public class MyScreen extends DeclarativeScreen {
 *     public MyScreen() { super(Component.literal("My UI")); }
 *
 *     protected void content(UIScope scope) {
 *         var count = Composition.current().remember(
 *             () -> new MutableState<>(0)
 *         );
 *         Column(scope, col -> {
 *             col.Text("Count: " + count.getValue());
 *             col.Button("+", () -> count.setValue(count.getValue() + 1));
 *         });
 *     }
 * }
 * }</pre>
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

    protected abstract void content(UIScope scope);

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (composition != null) {
            composition.renderFrame(extractor, this.width, this.height);
        }
    }

    // ── input routing ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) { // 左键
            for (UIComponent child : rootScope.getChildren()) {
                if (hitTest(child, lastMouseX, lastMouseY)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    /** 递归命中测试，找到最上层可点击组件并触发 click()。 */
    private boolean hitTest(UIComponent component, float px, float py) {
        // 先检查子组件（后绘制在上层，优先命中）
        var children = component.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (hitTest(children.get(i), px, py)) return true;
        }
        // 再检查自身
        if (component instanceof ButtonComponent btn) {
            if (btn.hitRect().contains(px, py)) {
                btn.click();
                return true;
            }
        }
        return false;
    }

    // ── internal ──

    private static class RootScope extends UIScope {
    }
}
