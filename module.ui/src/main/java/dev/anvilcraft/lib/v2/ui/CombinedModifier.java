package dev.anvilcraft.lib.v2.ui;

import java.util.function.BiFunction;

/**
 * Two {@link Modifier} chains joined by {@code then()}.
 */
record CombinedModifier(ModifierElement outer, Modifier inner) implements Modifier {

    @Override
    public Modifier then(Modifier other) {
        return new CombinedModifier(outer, inner.then(other));
    }

    @Override
    public <R> R foldIn(R initial, BiFunction<R, ModifierElement, R> operation) {
        return inner.foldIn(operation.apply(initial, outer), operation);
    }

    @Override
    public <R> R foldOut(R initial, BiFunction<ModifierElement, R, R> operation) {
        return operation.apply(outer, inner.foldOut(initial, operation));
    }
}
