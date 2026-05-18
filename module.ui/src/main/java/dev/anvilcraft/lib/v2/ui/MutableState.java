package dev.anvilcraft.lib.v2.ui;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * An observable state holder.
 * <p>
 * Reads are tracked against the current composition slot.
 * Writes mark all reader slots dirty so recomposition is scoped.
 *
 * @param <T> the type of value held
 */
public class MutableState<T> {

    private T value;
    final Set<Composition.Slot> readers = new HashSet<>();

    public MutableState(@Nullable T initialValue) {
        this.value = initialValue;
    }

    /**
     * Read the current value, recording this slot as a reader
     * if called within a composition emission.
     */
    @Nullable
    public T getValue() {
        Composition comp = Composition.currentOrNull();
        if (comp != null && comp.currentSlot != null) {
            comp.currentSlot.addReadState(this);
            readers.add(comp.currentSlot);
        }
        return value;
    }

    /**
     * Set a new value. If changed, marks all reader slots dirty.
     */
    public void setValue(@Nullable T newValue) {
        if (!Objects.equals(value, newValue)) {
            value = newValue;
            for (Composition.Slot slot : readers) {
                slot.markDirty();
            }
        }
    }

    @Override
    public String toString() {
        return "State(" + value + ")";
    }
}
