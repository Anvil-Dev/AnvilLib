package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Modifier;

import java.util.function.BiFunction;

@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public record CombinedModifier(ModifierElement outer, Modifier inner) implements Modifier {
    @Override
    public Modifier then(Modifier other) {
        return new CombinedModifier(this.outer(), this.inner().then(other));
    }

    @Override
    public <R> R foldIn(R initial, BiFunction<R, ModifierElement, R> operation) {
        return this.inner().foldIn(operation.apply(initial, this.outer()), operation);
    }

    @Override
    public <R> R foldOut(R initial, BiFunction<ModifierElement, R, R> operation) {
        return operation.apply(this.outer(), this.inner().foldOut(initial, operation));
    }
}
