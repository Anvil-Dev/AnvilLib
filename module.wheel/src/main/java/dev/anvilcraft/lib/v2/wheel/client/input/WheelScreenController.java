package dev.anvilcraft.lib.v2.wheel.client.input;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class WheelScreenController {
    @Nullable
    private WheelScreen currentHoldScreen;

    public void openTap(WheelMenuModel menuModel) {
        Minecraft.getInstance().setScreen(WheelScreen.tap(menuModel));
    }

    public void onHoldKeyPressed(WheelMenuModel menuModel) {
        if (this.currentHoldScreen != null) {
            return;
        }
        WheelScreen screen = WheelScreen.hold(menuModel);
        this.currentHoldScreen = screen;
        Minecraft.getInstance().setScreen(screen);
    }

    public void onHoldKeyReleased() {
        if (this.currentHoldScreen == null) {
            return;
        }
        WheelScreen screen = this.currentHoldScreen;
        this.currentHoldScreen = null;
        screen.triggerFromHoldRelease();
    }
}

