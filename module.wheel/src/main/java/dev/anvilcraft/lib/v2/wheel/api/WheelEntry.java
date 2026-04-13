package dev.anvilcraft.lib.v2.wheel.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class WheelEntry {
    private static final WheelEntryRenderer EMPTY_RENDERER = (graphics, pose, width, height) -> {
    };

    private final String id;
    private final Component label;
    private final WheelEntryRenderer renderer;
    @Nullable
    private final WheelEntryAction action;
    private final List<WheelEntry> submenu;
    private final boolean placeholder;

    private WheelEntry(
        String id,
        Component label,
        WheelEntryRenderer renderer,
        @Nullable WheelEntryAction action,
        List<WheelEntry> submenu,
        boolean placeholder
    ) {
        this.id = id;
        this.label = label;
        this.renderer = renderer;
        this.action = action;
        this.submenu = Collections.unmodifiableList(submenu);
        this.placeholder = placeholder;
    }

    public static WheelEntry action(String id, Component label, WheelEntryRenderer renderer, WheelEntryAction action) {
        Objects.requireNonNull(action, "action");
        return new WheelEntry(
            Objects.requireNonNull(id, "id"),
            Objects.requireNonNull(label, "label"),
            Objects.requireNonNull(renderer, "renderer"),
            action,
            List.of(),
            false
        );
    }

    public static WheelEntry submenu(String id, Component label, WheelEntryRenderer renderer, List<WheelEntry> submenu) {
        List<WheelEntry> submenuCopy = List.copyOf(Objects.requireNonNull(submenu, "submenu"));
        return new WheelEntry(
            Objects.requireNonNull(id, "id"),
            Objects.requireNonNull(label, "label"),
            Objects.requireNonNull(renderer, "renderer"),
            null,
            submenuCopy,
            false
        );
    }

    static WheelEntry placeholder(int slotIndex) {
        return new WheelEntry(
            "__empty_slot_" + slotIndex,
            Component.empty(),
            EMPTY_RENDERER,
            null,
            List.of(),
            true
        );
    }

    public String id() {
        return this.id;
    }

    public Component label() {
        return this.label;
    }

    public WheelEntryRenderer renderer() {
        return this.renderer;
    }

    public @Nullable WheelEntryAction action() {
        return this.action;
    }

    public List<WheelEntry> submenu() {
        return this.submenu;
    }

    public boolean hasSubmenu() {
        return !this.submenu.isEmpty();
    }

    public boolean isPlaceholder() {
        return this.placeholder;
    }

    public boolean isSelectable(WheelOpenMode mode) {
        if (this.placeholder) {
            return false;
        }
        if (mode == WheelOpenMode.HOLD && this.hasSubmenu()) {
            return false;
        }
        return this.action != null || this.hasSubmenu();
    }

    public static List<WheelEntry> mutableCopy(List<WheelEntry> entries) {
        return new ArrayList<>(Objects.requireNonNull(entries, "entries"));
    }
}

