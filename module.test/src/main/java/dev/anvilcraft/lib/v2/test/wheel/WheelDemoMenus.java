package dev.anvilcraft.lib.v2.test.wheel;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

public final class WheelDemoMenus {
    private WheelDemoMenus() {
    }

    public static WheelMenuModel buildTapDemo(int slotsPerPage, WheelSelectionEffect selectionEffect) {
        return WheelMenuBuilder.create()
            .slotsPerPage(slotsPerPage)
            .selectionEffect(selectionEffect)
            .action(
                "action_1", Component.literal("Action 1"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "action_2", Component.literal("Action 2"), WheelDemoMenus::render, ctx -> {
                }
            )
            .submenu(
                "tools", Component.literal("Tools"), WheelDemoMenus::render, submenu -> submenu
                    .action(
                        "tool_a", Component.literal("Tool A"), WheelDemoMenus::render, ctx -> {
                        }
                    )
                    .action(
                        "tool_b", Component.literal("Tool B"), WheelDemoMenus::render, ctx -> {
                        }
                    )
            )
            .action(
                "action_3", Component.literal("Action 3"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "action_4", Component.literal("Action 4"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "action_5", Component.literal("Action 5"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "action_6", Component.literal("Action 6"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "action_7", Component.literal("Action 7"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "action_8", Component.literal("Action 8"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "action_9", Component.literal("Action 9"), WheelDemoMenus::render, ctx -> {
                }
            )
            .build();
    }

    public static WheelMenuModel buildHoldDemo(int slotsPerPage, WheelSelectionEffect selectionEffect) {
        return WheelMenuBuilder.create()
            .slotsPerPage(slotsPerPage)
            .selectionEffect(selectionEffect)
            .action(
                "hold_1", Component.literal("Hold 1"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "hold_2", Component.literal("Hold 2"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "hold_3", Component.literal("Hold 3"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "hold_4", Component.literal("Hold 4"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "hold_5", Component.literal("Hold 5"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "hold_6", Component.literal("Hold 6"), WheelDemoMenus::render, ctx -> {
                }
            )
            .action(
                "hold_7", Component.literal("Hold 7"), WheelDemoMenus::render, ctx -> {
                }
            )
            .submenu(
                "ignored_submenu", Component.literal("Ignored Submenu"), WheelDemoMenus::render, submenu -> submenu
                    .action(
                        "never_opened", Component.literal("Never Opened"), WheelDemoMenus::render, ctx -> {
                        }
                    )
            )
            .action(
                "hold_8", Component.literal("Hold 8"), WheelDemoMenus::render, ctx -> {
                }
            )
            .build();
    }

    public static void render(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, int width, int height) {
        graphics.item(Items.APPLE.getDefaultInstance(), -8, -8);
    }
}
