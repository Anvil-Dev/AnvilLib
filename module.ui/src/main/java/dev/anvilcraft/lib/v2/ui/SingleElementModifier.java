package dev.anvilcraft.lib.v2.ui;

import java.util.function.BiFunction;

/**
 * A modifier chain node wrapping a single {@link ModifierElement}.
 */
record SingleElementModifier(ModifierElement element) implements Modifier {

    @Override
    public Modifier then(Modifier other) {
        return new CombinedModifier(element, other);
    }

    @Override
    public <R> R foldIn(R initial, BiFunction<R, ModifierElement, R> operation) {
        return operation.apply(initial, element);
    }

    @Override
    public <R> R foldOut(R initial, BiFunction<ModifierElement, R, R> operation) {
        return operation.apply(element, initial);
    }
}
