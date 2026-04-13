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
     * Tick 检测 TAP 按键（consumeClick 确保每次按下只触发一次）
     * 以及 HOLD 按键的按下/松开边沿。
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            // 有 Screen 时不处理 TAP，但 HOLD 松开要继续检测
            if (holdKeyWasDown && !WheelTestKeys.HOLD_KEY.isDown()) {
                CONTROLLER.onHoldKeyReleased();
                holdKeyWasDown = false;
            }
            return;
        }

        // --- TAP ---
        while (WheelTestKeys.TAP_KEY.consumeClick()) {
            CONTROLLER.openTap(WheelDemoMenus.buildTapDemo(8));
        }

        // --- HOLD ---
        boolean holdKeyDown = WheelTestKeys.HOLD_KEY.isDown();
        if (holdKeyDown && !holdKeyWasDown) {
            CONTROLLER.onHoldKeyPressed(WheelDemoMenus.buildHoldDemo(8));
        } else if (!holdKeyDown && holdKeyWasDown) {
            CONTROLLER.onHoldKeyReleased();
        }
        holdKeyWasDown = holdKeyDown;
    }

    /**
     * 通过 InputEvent.Key 处理 HOLD 松开（Screen 已经开着时也能捕获）
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_RELEASE
            && WheelTestKeys.HOLD_KEY.matches(event.getKey(), event.getScanCode())) {
            if (holdKeyWasDown) {
                CONTROLLER.onHoldKeyReleased();
                holdKeyWasDown = false;
            }
        }
    }
}

