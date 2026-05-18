package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Modifier;

import java.util.function.BiFunction;

public record CombinedModifier(ModifierElement outer, Modifier inner) implements Modifier {
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
