package dev.anvilcraft.lib.v2.wheel.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WheelMenuModel {
    public static final int DEFAULT_SLOTS_PER_PAGE = 8;

    private final List<WheelEntry> rootEntries;
    private final int slotsPerPage;

    private WheelMenuModel(List<WheelEntry> rootEntries, int slotsPerPage) {
        if (slotsPerPage < 1) {
            throw new IllegalArgumentException("slotsPerPage must be >= 1");
        }
        this.rootEntries = List.copyOf(Objects.requireNonNull(rootEntries, "rootEntries"));
        this.slotsPerPage = slotsPerPage;
    }

    public static WheelMenuModel of(List<WheelEntry> rootEntries) {
        return new WheelMenuModel(rootEntries, DEFAULT_SLOTS_PER_PAGE);
    }

    public static WheelMenuModel of(List<WheelEntry> rootEntries, int slotsPerPage) {
        return new WheelMenuModel(rootEntries, slotsPerPage);
    }

    public List<WheelEntry> rootEntries() {
        return this.rootEntries;
    }

    public int slotsPerPage() {
        return this.slotsPerPage;
    }

    public int pageCount(List<WheelEntry> entries) {
        List<WheelEntry> safeEntries = Objects.requireNonNull(entries, "entries");
        return WheelPagination.pageCount(safeEntries.size(), this.slotsPerPage);
    }

    public int pageCount() {
        return this.pageCount(this.rootEntries);
    }

    public WheelPageModel page(List<WheelEntry> entries, int pageIndex) {
        List<WheelEntry> safeEntries = Objects.requireNonNull(entries, "entries");
        int pages = this.pageCount(safeEntries);
        if (pageIndex < 0 || pageIndex >= pages) {
            throw new IllegalArgumentException("pageIndex out of range: " + pageIndex);
        }
        int start = pageIndex * this.slotsPerPage;
        int end = Math.min(start + this.slotsPerPage, safeEntries.size());
        List<WheelEntry> slots = new ArrayList<>(this.slotsPerPage);
        for (int i = start; i < end; i++) {
            slots.add(safeEntries.get(i));
        }
        for (int i = slots.size(); i < this.slotsPerPage; i++) {
            slots.add(WheelEntry.placeholder(i));
        }
        return new WheelPageModel(pageIndex, this.slotsPerPage, slots);
    }

    public WheelPageModel page(int pageIndex) {
        return this.page(this.rootEntries, pageIndex);
    }
}

