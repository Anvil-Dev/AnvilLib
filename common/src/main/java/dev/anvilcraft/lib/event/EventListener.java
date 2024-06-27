package dev.anvilcraft.lib.event;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

class EventListener<E> implements Consumer<E> {
    private final Consumer<E> consumer;
    private final int priority;

    public EventListener(Consumer<E> consumer, int priority) {
        this.consumer = consumer;
        this.priority = priority;
    }

    @Override
    public void accept(E e) {
        consumer.accept(e);
    }

    public static int compare(@NotNull EventListener<?> o1, @NotNull EventListener<?> o2) {
        return o1.priority - o2.priority;
    }
}
