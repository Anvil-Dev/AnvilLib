package dev.anvilcraft.lib.v2.wheel.api;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class WheelMenuBuilder {
    private final List<WheelEntry> entries = new ArrayList<>();
    private int slotsPerPage = WheelMenuModel.DEFAULT_SLOTS_PER_PAGE;
    private int deadZone = 15;
    private WheelSelectionEffect selectionEffect = WheelSelectionEffect.ANNULAR_SECTOR;
    private int selectionEffectColor = WheelMenuModel.DEFAULT_SELECTION_EFFECT_COLOR;

    private WheelMenuBuilder() {
    }

    public static WheelMenuBuilder create() {
        return new WheelMenuBuilder();
    }

    public WheelMenuBuilder slotsPerPage(int slotsPerPage) {
        if (slotsPerPage < 1) {
            throw new IllegalArgumentException("slotsPerPage must be >= 1");
        }
        this.slotsPerPage = slotsPerPage;
        return this;
    }

    public WheelMenuBuilder deadZone(int deadZone){
        this.deadZone = deadZone;
        return this;
    }

    public WheelMenuBuilder selectionEffect(WheelSelectionEffect selectionEffect) {
        this.selectionEffect = Objects.requireNonNull(selectionEffect, "selectionEffect");
        return this;
    }

    public WheelMenuBuilder selectionEffectColor(int selectionEffectColor) {
        this.selectionEffectColor = selectionEffectColor;
        return this;
    }

    public WheelMenuBuilder action(
        String id,
        Component label,
        WheelEntryAction action
    ) {
        this.entries.add(WheelEntry.action(id, label, action));
        return this;
    }

    public WheelMenuBuilder action(
        String id,
        Component label,
        WheelEntryRenderer renderer,
        WheelEntryAction action
    ) {
        this.entries.add(WheelEntry.action(id, label, renderer, action));
        return this;
    }

    public WheelMenuBuilder submenu(
        String id,
        Component label,
        Consumer<WheelSubmenuBuilder> submenuBuilder
    ) {
        WheelSubmenuBuilder builder = new WheelSubmenuBuilder();
        Objects.requireNonNull(submenuBuilder, "submenuBuilder").accept(builder);
        this.entries.add(WheelEntry.submenu(id, label, builder.buildEntries()));
        return this;
    }

    public WheelMenuBuilder submenu(
        String id,
        Component label,
        WheelEntryRenderer renderer,
        Consumer<WheelSubmenuBuilder> submenuBuilder
    ) {
        WheelSubmenuBuilder builder = new WheelSubmenuBuilder();
        Objects.requireNonNull(submenuBuilder, "submenuBuilder").accept(builder);
        this.entries.add(WheelEntry.submenu(id, label, renderer, builder.buildEntries()));
        return this;
    }

    public WheelMenuModel build() {
        return WheelMenuModel.of(
            List.copyOf(this.entries),
            this.slotsPerPage,
            this.deadZone,
            this.selectionEffect,
            this.selectionEffectColor
        );
    }

    public static final class WheelSubmenuBuilder {
        private final List<WheelEntry> entries = new ArrayList<>();

        public WheelSubmenuBuilder action(
            String id,
            Component label,
            WheelEntryAction action
        ) {
            this.entries.add(WheelEntry.action(id, label, action));
            return this;
        }

        public WheelSubmenuBuilder action(
            String id,
            Component label,
            WheelEntryRenderer renderer,
            WheelEntryAction action
        ) {
            this.entries.add(WheelEntry.action(id, label, renderer, action));
            return this;
        }

        private List<WheelEntry> buildEntries() {
            return List.copyOf(this.entries);
        }
    }
}

