package dev.anvilcraft.lib.v2.test.port;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import dev.anvilcraft.lib.v2.font.extension.GuiGraphicsExtension;
import dev.anvilcraft.lib.v2.test.wheel.WheelDemoMenus;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 实际提交轮盘、毛玻璃与 SDF 字体绘制，截图供人工核对。 */
public final class PortGraphicsVerification {
    private static int ticks;
    private static int rendered;

    public static boolean tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (++ticks == 1) {
            minecraft.setScreen(new WheelScreen(WheelDemoMenus.buildTapDemo(8, WheelSelectionEffect.ANNULAR_SECTOR),
                dev.anvilcraft.lib.v2.wheel.api.WheelOpenMode.TAP) {
                @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
                    super.render(graphics, width / 2 + 60, height / 2, partial);
                }
            });
        }
        if (ticks == 20) screenshot("port-wheel.png");
        if (ticks == 22) minecraft.setScreen(new Screen(Component.literal("AnvilLib 字体验证")) {
            @Override public boolean isPauseScreen() { return false; }
            @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
                graphics.fill(0, 0, width, height, 0xFF16202C);
                GuiGraphicsExtension extension = (GuiGraphicsExtension) graphics;
                extension.anvillib$centeredText(AnvilLibFont.getSelectFont(), Component.literal("AnvilLib 1.21.11"), width / 2, height / 2 - 20, -1);
                extension.anvillib$centeredText(AnvilLibFont.getSelectFont(), Component.literal("字体渲染验证 0123456789"), width / 2, height / 2, 0xFF73D6A1);
                rendered++;
            }
        });
        if (ticks == 40) screenshot("port-font.png");
        if (ticks < 42) return false;
        PortVerification.check(rendered > 0, "SDF 字体必须经过实际 GUI 绘制");
        minecraft.setScreen(null);
        return true;
    }

    private static void screenshot(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        net.minecraft.client.Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(), 1, message -> { });
    }
}
