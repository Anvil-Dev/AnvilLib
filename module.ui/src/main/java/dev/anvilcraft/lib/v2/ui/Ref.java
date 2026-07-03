package dev.anvilcraft.lib.v2.ui;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
public class Ref<T extends @Nullable Object> implements Supplier<T>, Consumer<T> {
    final Set<Composition.Slot> readers = new HashSet<>();
    final Map<Long, Consumer<Ref<T>>> observers = new HashMap<>();
    private long nextObserverId = 0;
    private T value;

    public Ref(T initialValue) {
        this.value = initialValue;
    }

    /**
     * 读取当前值。若在 composition emission 期间调用，记录此 slot 为 reader。
     */
    public T get() {
        Composition comp = Composition.currentOrNull();
        if (comp != null && comp.currentSlot != null) {
            comp.currentSlot.addReadState(this);
            this.readers.add(comp.currentSlot);
        }
        return this.value;
    }

    /**
     * 设置新值。若值发生变化，标记所有 reader slot 为脏，并通知 observer。
     */
    public void accept(T newValue) {
        if (!Objects.equals(this.value, newValue)) {
            this.value = newValue;
            for (Composition.Slot slot : this.readers) {
                slot.markDirty();
            }
            for (Consumer<Ref<T>> observer : this.observers.values()) {
                observer.accept(this);
            }
        }
    }

    /**
     * 移除指定 slot 的 reader 追踪。由 Composition 在 slot 被回收时调用。
     */
    void removeReader(Composition.Slot slot) {
        this.readers.remove(slot);
    }

    @Override
    public String toString() {
        return "State(" + this.value + ")";
    }

    /**
     * 注册值变化观察者。返回 ID 用于后续取消。
     */
    public Long watch(Consumer<Ref<T>> watcher) {
        long id = this.nextObserverId++;
        this.observers.put(id, watcher);
        return id;
    }

    public Consumer<Ref<T>> unwatch(Long id) {
        return this.observers.remove(id);
    }

    /**
     * 创建只读映射属性。源变化时自动触发 recompose。
     */
    public <R> Ref<R> map(Function<T, R> mapper) {
        Ref<R> mapped = new Ref<>(mapper.apply(this.value));
        this.watch(src -> mapped.accept(mapper.apply(src.get())));
        return mapped;
    }

    /**
     * 双向绑定两个 Ref。一方变化时自动同步另一方。
     */
    public void bindBidirectional(Ref<T> other) {
        this.watch(src -> {
            if (!Objects.equals(other.get(), src.get())) {
                other.accept(src.get());
            }
        });
        other.watch(src -> {
            if (!Objects.equals(this.get(), src.get())) {
                this.accept(src.get());
            }
        });
    }
}
