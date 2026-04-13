package dev.anvilcraft.lib.v2.wheel.api;

@FunctionalInterface
public interface WheelEntryAction {
    void trigger(WheelActionContext context);
}

