package dev.anvilcraft.lib.v2.ui;

import java.util.function.BiFunction;

/**
 * Chainable modifier API. Modifiers form a linked list via {@link #then}.
 * <p>
 * Each modifier element can participate in measure, layout, and render phases.
 * Callers fold over the chain with {@link #foldIn} / {@link #foldOut}.
 */
public interface Modifier {

    Modifier then(Modifier other);

    <R> R foldIn(R initial, BiFunction<R, ModifierElement, R> operation);

    <R> R foldOut(R initial, BiFunction<ModifierElement, R, R> operation);

    /** Return a new chain with the given element prepended. */
    default Modifier prepend(ModifierElement element) {
        return then(new SingleElementModifier(element));
    }

    // ── factory shortcuts ──

    default Modifier size(float width, float height) {
        return prepend(ModifierElement.size(width, height));
    }

    default Modifier width(float width) {
        return prepend(ModifierElement.width(width));
    }

    default Modifier height(float height) {
        return prepend(ModifierElement.height(height));
    }

    default Modifier fillMaxWidth() {
        return prepend(ModifierElement.fillMaxWidth());
    }

    default Modifier fillMaxHeight() {
        return prepend(ModifierElement.fillMaxHeight());
    }

    default Modifier fillMaxSize() {
        return prepend(ModifierElement.fillMaxSize());
    }

    default Modifier padding(float all) {
        return prepend(ModifierElement.padding(all));
    }

    default Modifier padding(float horizontal, float vertical) {
        return prepend(ModifierElement.padding(horizontal, vertical));
    }

    default Modifier background(int color) {
        return prepend(ModifierElement.background(color));
    }

    Modifier NONE = new Modifier() {
        @Override
        public Modifier then(Modifier other) {
            return other;
        }

        @Override
        public <R> R foldIn(R initial, BiFunction<R, ModifierElement, R> operation) {
            return initial;
        }

        @Override
        public <R> R foldOut(R initial, BiFunction<ModifierElement, R, R> operation) {
            return initial;
        }
    };
}
