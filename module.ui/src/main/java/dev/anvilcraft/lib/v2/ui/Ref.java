package dev.anvilcraft.lib.v2.ui;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 可观察状态持有者。
 * <p>
 * 读取时追踪当前 composition slot，写入时标记所有 reader slot 为脏，
 * 从而实现精确范围的 recompose。
 *
 * @param <T> 持有值的类型
 */
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class Ref<T> {
    final Set<Composition.Slot> readers = new HashSet<>();
    private @Nullable T value;

    public Ref(@Nullable T initialValue) {
        this.value = initialValue;
    }

    /**
     * 读取当前值。若在 composition emission 期间调用，记录此 slot 为 reader。
     */
    @Nullable
    public T getValue() {
        Composition comp = Composition.currentOrNull();
        if (comp != null && comp.currentSlot != null) {
            comp.currentSlot.addReadState(this);
            this.readers.add(comp.currentSlot);
        }
        return this.value;
    }

    /**
     * 设置新值。若值发生变化，标记所有 reader slot 为脏。
     */
    public void setValue(@Nullable T newValue) {
        if (!Objects.equals(this.value, newValue)) {
            this.value = newValue;
            for (Composition.Slot slot : this.readers) {
                slot.markDirty();
            }
        }
    }

    @Override
    public String toString() {
        return "State(" + this.value + ")";
    }
}
