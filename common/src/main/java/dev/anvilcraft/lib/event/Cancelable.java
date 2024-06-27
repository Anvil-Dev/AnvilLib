package dev.anvilcraft.lib.event;

@SuppressWarnings("unused")
public interface Cancelable {
    boolean isCanceled();

    void cancel();
}
