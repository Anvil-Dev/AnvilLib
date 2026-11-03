package dev.anvilcraft.lib.v2.test.flexible;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * 柔性控件回归的客户端入口。
 *
 * <p>交互方式有两条：{@code /anvillibtest flexible} 客户端命令与 {@code F7} 快捷键，两者都打开
 * {@link FlexibleTestScreen}，由屏幕在初始化时完成贴图核对。
 *
 * <p>自动化方式复用 {@code anvillib.flexibleUiTest} 系统属性：置位后客户端在进入主菜单时隐藏窗口、
 * 自动打开回归屏幕，把核对结论写入游戏目录下的 {@code flexible-result.txt} 并退出。
 * 隐藏窗口让该流程不会抢占用户正在使用的桌面，核对结论同时写入日志。
 */
@EventBusSubscriber(modid = AnvilLibTest.MOD_ID, value = Dist.CLIENT)
public final class FlexibleTestClient {
    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(AnvilLibTest.of("flexible"));
    public static final KeyMapping OPEN_SCREEN = new KeyMapping(
        "key.anvillib_test.flexible_screen",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_F7,
        FlexibleTestClient.KEY_CATEGORY
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESULT_FILE = "flexible-result.txt";
    private static final boolean AUTOMATED = Boolean.getBoolean("anvillib.flexibleUiTest");
    private static int autoStage;

    private FlexibleTestClient() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FlexibleTestClient.OPEN_SCREEN);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("anvillibtest").then(
                Commands.literal("flexible").executes(context -> {
                    FlexibleTestClient.open();
                    context.getSource().sendSuccess(
                        () -> Component.literal("Opened the flexible control screen"),
                        false
                    );
                    return 1;
                })
            )
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        FlexibleTestClient.runAutomated();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        while (FlexibleTestClient.OPEN_SCREEN.consumeClick()) {
            FlexibleTestClient.open();
        }
    }

    private static void runAutomated() {
        if (!FlexibleTestClient.AUTOMATED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.overlay() != null) {
            return;
        }
        switch (FlexibleTestClient.autoStage) {
            case 0 -> {
                GLFW.glfwHideWindow(minecraft.getWindow().handle());
                FlexibleTestClient.open();
                FlexibleTestClient.autoStage = 1;
            }
            case 1 -> {
                if (!(minecraft.gui.screen() instanceof FlexibleTestScreen screen)) {
                    return;
                }
                List<String> failures = screen.failures();
                String line = failures.isEmpty()
                    ? "PASS " + screen.checkCount()
                    : "FAIL " + failures.size() + " of " + screen.checkCount() + " " + failures;
                FlexibleTestClient.writeResult(minecraft, line);
                FlexibleTestClient.autoStage = 2;
                minecraft.stop();
            }
            default -> {
            }
        }
    }

    private static void writeResult(Minecraft minecraft, String line) {
        try {
            Files.writeString(minecraft.gameDirectory.toPath().resolve(FlexibleTestClient.RESULT_FILE), line);
            FlexibleTestClient.LOGGER.info("FLEXIBLE_UI_TEST {}", line);
        } catch (IOException exception) {
            FlexibleTestClient.LOGGER.error("Unable to write {}", FlexibleTestClient.RESULT_FILE, exception);
        }
    }

    private static void open() {
        Minecraft.getInstance().gui.setScreen(new FlexibleTestScreen());
    }
}
