package dev.anvilcraft.lib.v2.wheel.api;

import java.util.List;

public record WheelPageModel(
    int pageIndex,
    int slotsPerPage,
    List<WheelEntry> slots
) {
    public WheelPageModel {
        slots = List.copyOf(slots);
        if (slotsPerPage < 1) {
            throw new IllegalArgumentException("slotsPerPage must be >= 1");
        }
        if (slots.size() != slotsPerPage) {
            throw new IllegalArgumentException("slots size must equal slotsPerPage");
        }
    }

    public WheelEntry slot(int slotIndex) {
        return this.slots.get(slotIndex);
    }

    public boolean isSelectable(int slotIndex, WheelOpenMode mode) {
        return this.slot(slotIndex).isSelectable(mode);
    }
}

