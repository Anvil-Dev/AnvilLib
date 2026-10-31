package dev.anvilcraft.lib.v2.test.config;

import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.config.ConfigScreenStyle;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.neoforged.fml.config.ModConfig;

/** 两种风格混用，用于文件切换和默认界面兼容性验收。 */
public final class ConfigUiExamples {
    private ConfigUiExamples() {}

    public static void register() {
        ConfigManager.register(AnvilLibTest.MOD_ID, Client::new);
        ConfigManager.register(AnvilLibTest.MOD_ID, Legacy::new);
        ConfigManager.register(AnvilLibTest.MOD_ID, Server::new);
    }

    @Config(name = AnvilLibTest.MOD_ID, group = "anvillib", type = ModConfig.Type.CLIENT,
        screenStyle = ConfigScreenStyle.SIDEBAR)
    public static class Client {
        public boolean showHints = true;
        public String greeting = "Welcome";
        @CollapsibleObject
        public Appearance appearance = new Appearance();
    }

    public static class Appearance {
        public boolean compact = false;
        public int spacing = 8;
    }

    @Config(name = "anvillib_test_legacy", group = "anvillib")
    public static class Legacy {
        public boolean useDefaultScreen = true;
    }

    @Config(name = AnvilLibTest.MOD_ID, group = "anvillib", type = ModConfig.Type.SERVER,
        screenStyle = ConfigScreenStyle.SIDEBAR)
    public static class Server {
        public boolean allowEditing = true;
    }
}
