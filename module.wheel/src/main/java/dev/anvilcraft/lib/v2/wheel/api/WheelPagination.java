package dev.anvilcraft.lib.v2.wheel.api;

public final class WheelPagination {
    private WheelPagination() {
    }

    public static int pageCount(int entryCount, int slotsPerPage) {
        if (slotsPerPage < 1) {
            throw new IllegalArgumentException("slotsPerPage must be >= 1");
        }
        if (entryCount <= 0) {
            return 1;
        }
        return (entryCount + slotsPerPage - 1) / slotsPerPage;
    }
}

