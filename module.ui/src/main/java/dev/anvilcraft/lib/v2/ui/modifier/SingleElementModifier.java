package dev.anvilcraft.lib.v2.ui.modifier;

import dev.anvilcraft.lib.v2.ui.Modifier;

import java.util.function.BiFunction;

public record SingleElementModifier(ModifierElement element) implements Modifier {
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
