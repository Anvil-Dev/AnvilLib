package dev.anvilcraft.lib.v2.wheel.api;

public record WheelActionContext(
    int pageIndex,
    int slotIndex,
    String itemId,
    WheelOpenMode openMode
) {
}

