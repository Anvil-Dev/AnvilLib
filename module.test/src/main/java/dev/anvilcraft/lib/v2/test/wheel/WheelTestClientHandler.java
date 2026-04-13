package dev.anvilcraft.lib.v2.test.wheel;

import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AnvilLibTest.MOD_ID, value = Dist.CLIENT)
public final class WheelTestClientHandler {
    private static final WheelScreenController CONTROLLER = new WheelScreenController();
    private static boolean holdKeyWasDown = false;

    private WheelTestClientHandler() {
    }

    /**
     * Tick 只处理 TAP，避免在 Screen 打开时通过 isDown 误判 HOLD 状态导致闪烁。
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        // --- TAP ---
        while (WheelTestKeys.TAP_KEY.consumeClick()) {
            CONTROLLER.openTap(WheelDemoMenus.buildTapDemo(8));
        }
    }

    /**
     * 通过按键事件处理 HOLD 的按下/松开边沿，避免 tick 轮询造成开关抖动。
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !WheelTestKeys.HOLD_KEY.matches(event.getKey(), event.getScanCode())) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (!holdKeyWasDown) {
                CONTROLLER.onHoldKeyPressed(WheelDemoMenus.buildHoldDemo(8));
                holdKeyWasDown = true;
            }
            return;
        }
        if (event.getAction() == GLFW.GLFW_RELEASE && holdKeyWasDown) {
            CONTROLLER.onHoldKeyReleased();
            holdKeyWasDown = false;
        }
    }
}

