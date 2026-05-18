package dev.anvilcraft.lib.v2.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
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

    protected DeclarativeScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        composition = new Composition(rootScope);
        composition.setContent(this::content);
    }

    /**
     * Declare the UI content. Called on initial composition and every recomposition.
     * Use {@link Composition#current()} to access state helpers like {@code remember}.
     */
    protected abstract void content(UIScope scope);

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        if (composition != null) {
            composition.renderFrame(extractor, this.width, this.height);
        }
    }

    // ── input routing (stub — Phase 5 adds full hit-testing) ──

    // TODO Phase 5: override mouseClicked(MouseButtonEvent, boolean), keyPressed(KeyEvent)

    // ── internal ──

    private static class RootScope extends UIScope {
    }
}
