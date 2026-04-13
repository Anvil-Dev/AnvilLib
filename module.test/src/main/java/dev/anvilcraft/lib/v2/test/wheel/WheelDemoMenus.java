package dev.anvilcraft.lib.v2.test.wheel;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import net.minecraft.network.chat.Component;

public final class WheelDemoMenus {
    private WheelDemoMenus() {
    }

    public static WheelMenuModel buildTapDemo(int slotsPerPage) {
        return WheelMenuBuilder.create()
            .slotsPerPage(slotsPerPage)
            .action("action_1", Component.literal("Action 1"), (g, p, w, h) -> {}, ctx -> {})
            .action("action_2", Component.literal("Action 2"), (g, p, w, h) -> {}, ctx -> {})
            .submenu("tools", Component.literal("Tools"), (g, p, w, h) -> {}, submenu -> submenu
                .action("tool_a", Component.literal("Tool A"), (g, p, w, h) -> {}, ctx -> {})
                .action("tool_b", Component.literal("Tool B"), (g, p, w, h) -> {}, ctx -> {})
            )
            .action("action_3", Component.literal("Action 3"), (g, p, w, h) -> {}, ctx -> {})
            .build();
    }

    public static WheelMenuModel buildHoldDemo(int slotsPerPage) {
        return WheelMenuBuilder.create()
            .slotsPerPage(slotsPerPage)
            .action("hold_1", Component.literal("Hold 1"), (g, p, w, h) -> {}, ctx -> {})
            .submenu("ignored_submenu", Component.literal("Ignored Submenu"), (g, p, w, h) -> {}, submenu -> submenu
                .action("never_opened", Component.literal("Never Opened"), (g, p, w, h) -> {}, ctx -> {})
            )
            .action("hold_2", Component.literal("Hold 2"), (g, p, w, h) -> {}, ctx -> {})
            .build();
    }
}

