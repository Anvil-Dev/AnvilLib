package dev.anvilcraft.lib.v2.wheel.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WheelMenuModel {
    public static final int DEFAULT_SLOTS_PER_PAGE = 8;
    public static final int DEFAULT_SELECTION_EFFECT_COLOR = 0xFFFABC02;

    private final List<WheelEntry> rootEntries;
    private final int slotsPerPage;
    private final int deadZone;
    private final WheelSelectionEffect selectionEffect;
    private final int selectionEffectColor;

    private WheelMenuModel(
        List<WheelEntry> rootEntries,
        int slotsPerPage,
        int deadZone,
        WheelSelectionEffect selectionEffect,
        int selectionEffectColor
    ) {
        if (slotsPerPage < 1) {
            throw new IllegalArgumentException("slotsPerPage must be >= 1");
        }
        this.rootEntries = List.copyOf(Objects.requireNonNull(rootEntries, "rootEntries"));
        this.slotsPerPage = slotsPerPage;
        this.deadZone = deadZone;
        this.selectionEffect = Objects.requireNonNull(selectionEffect, "selectionEffect");
        this.selectionEffectColor = selectionEffectColor;
    }

    public static WheelMenuModel of(List<WheelEntry> rootEntries) {
        return new WheelMenuModel(rootEntries, DEFAULT_SLOTS_PER_PAGE, 30, WheelSelectionEffect.DOT, DEFAULT_SELECTION_EFFECT_COLOR);
    }

    public static WheelMenuModel of(List<WheelEntry> rootEntries, int slotsPerPage, int deadZone) {
        return new WheelMenuModel(rootEntries, slotsPerPage, deadZone, WheelSelectionEffect.DOT, DEFAULT_SELECTION_EFFECT_COLOR);
    }

    public static WheelMenuModel of(
        List<WheelEntry> rootEntries,
        int slotsPerPage,
        int deadZone,
        WheelSelectionEffect selectionEffect
    ) {
        return new WheelMenuModel(rootEntries, slotsPerPage, deadZone, selectionEffect, DEFAULT_SELECTION_EFFECT_COLOR);
    }

    public static WheelMenuModel of(
        List<WheelEntry> rootEntries,
        int slotsPerPage,
        int deadZone,
        WheelSelectionEffect selectionEffect,
        int selectionEffectColor
    ) {
        return new WheelMenuModel(rootEntries, slotsPerPage, deadZone, selectionEffect, selectionEffectColor);
    }

    public List<WheelEntry> rootEntries() {
        return this.rootEntries;
    }

    public int slotsPerPage() {
        return this.slotsPerPage;
    }

    public int deadZone() {
        return this.deadZone;
    }

    public WheelSelectionEffect selectionEffect() {
        return this.selectionEffect;
    }

    public int selectionEffectColor() {
        return this.selectionEffectColor;
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

