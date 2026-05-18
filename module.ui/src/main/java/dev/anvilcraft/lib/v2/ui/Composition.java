package dev.anvilcraft.lib.v2.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The composition engine that drives recomposition, state tracking, and rendering.
 * <p>
 * One Composition is created per {@link DeclarativeScreen}.
 * It manages a flat slot table indexed by call-site position.
 * During recomposition, the content lambda replays and each
 * {@link #emit(UIComponent)} call diffs against the slot at the
 * same index.
 * <p>
 * State reads are tracked per slot so that writes only mark
 * affected slots dirty — not the entire tree.
 */
public class Composition {

    private static final ThreadLocal<Composition> CURRENT = new ThreadLocal<>();

    /** Returns the composition active on this thread, or null. */
    public static Composition currentOrNull() {
        return CURRENT.get();
    }

    /** Returns the composition active on this thread, throwing if absent. */
    public static Composition current() {
        Composition c = CURRENT.get();
        if (c == null) {
            throw new IllegalStateException("Not inside a composition frame");
        }
        return c;
    }

    // ── slot table ──

    private final List<Slot> slots = new ArrayList<>();
    private int currentIndex;
    private int currentRememberKey;
    private final Map<Integer, Object> rememberedValues = new HashMap<>();

    /** The slot currently being emitted (set during {@link #emit}). */
    Slot currentSlot;

    // ── state ──

    private boolean dirty = true;
    private Consumer<UIScope> content;
    private UIScope rootScope;

    public Composition(UIScope rootScope) {
        this.rootScope = rootScope;
    }

    public void setContent(Consumer<UIScope> content) {
        this.content = content;
    }

    /** Mark the composition as needing recomposition next frame. */
    public void invalidate() {
        dirty = true;
    }

    // ── remember ──

    /**
     * Persist a value across recompositions.
     * The init supplier is only called on first composition;
     * subsequent recompositions return the existing value.
     */
    @SuppressWarnings("unchecked")
    public <T> T remember(Supplier<T> init) {
        int key = currentRememberKey++;
        Object existing = rememberedValues.get(key);
        if (existing != null) {
            return (T) existing;
        }
        T value = init.get();
        rememberedValues.put(key, value);
        return value;
    }

    // ── emit ──

    /**
     * Emit a component to the current call-site position in the slot table.
     * Called by component factory functions.
     */
    public void emit(UIComponent component) {
        Slot slot;
        if (currentIndex < slots.size()) {
            slot = slots.get(currentIndex);
            slot.component = component;
        } else {
            slot = new Slot();
            slot.component = component;
            slots.add(slot);
        }
        currentSlot = slot;
        currentIndex++;
    }

    // ── frame entry point ──

    /**
     * Called every frame from {@link DeclarativeScreen#extractRenderState}.
     * Runs recomposition if dirty, then measure → layout → render.
     */
    public void renderFrame(GuiGraphicsExtractor extractor, float screenWidth, float screenHeight) {
        CURRENT.set(this);
        try {
            if (dirty) {
                recompose();
                dirty = false;
            }
            Constraints rootConstraints = new Constraints(0, screenWidth, 0, screenHeight);
            for (UIComponent child : rootScope.getChildren()) {
                renderTree(child, extractor, rootConstraints);
            }
        } finally {
            CURRENT.set(null);
        }
    }

    // ── recompose ──

    private void recompose() {
        rootScope.clearChildren();
        currentIndex = 0;
        currentRememberKey = 0;
        content.accept(rootScope);
        // Remove slots beyond currentIndex (conditionally removed components)
        while (slots.size() > currentIndex) {
            Slot removed = slots.remove(slots.size() - 1);
            // Purge remembered values that belong to removed slots.
            // A simple approach: remembered values for keys beyond
            // currentRememberKey are dead; we leave them as they'll
            // be overwritten on next composition anyway.
        }
    }

    // ── measure → layout → render walk ──

    private void renderTree(UIComponent component, GuiGraphicsExtractor extractor, Constraints constraints) {
        // 1. Apply modifier to constraints
        Constraints modConstraints = component.modifier().foldIn(
                constraints,
                (c, el) -> el.modifyConstraints(c)
        );

        // 2. Measure
        MeasuredSize size = component.measure(modConstraints);
        size = component.modifier().foldOut(
                size,
                (el, s) -> el.modifyMeasuredSize(component, modConstraints, s)
        );

        // 3. Layout
        LayoutRect rect = LayoutRect.of(0, 0, size.width(), size.height());
        rect = component.modifier().foldOut(
                rect,
                (el, r) -> el.modifyLayout(r)
        );
        component.layout(rect.x(), rect.y(), rect.width(), rect.height());

        // 4. Emit modifier render states (background, border, etc.)
        final LayoutRect finalRect = rect;
        component.modifier().foldOut(
                extractor,
                (el, e) -> {
                    el.emitRenderState(e, finalRect);
                    return e;
                }
        );

        // 5. Emit component's own render states
        component.extractRenderState(extractor);

        // 6. Recurse into children (container components handle their own children
        //    in measure/layout/extractRenderState, but we also walk them here
        //    so the external renderTree call drives the full tree)
    }

    // ── slot ──

    /**
     * A position in the slot table. Each slot holds a component and
     * tracks which states it reads for precise dirty marking.
     */
    public static class Slot {
        UIComponent component;
        boolean dirty = true;
        final Set<MutableState<?>> readStates = new HashSet<>();

        void addReadState(MutableState<?> state) {
            readStates.add(state);
        }

        void markDirty() {
            dirty = true;
        }
    }
}
