package dev.anvilcraft.lib.v2.test.config;

import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Slider;
import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.config.ConfigScreenStyle;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.neoforged.fml.config.ModConfig;

/** 两种风格混用，用于文件切换和默认界面兼容性验收。 */
public final class ConfigUiExamples {
    public static Client client;
    public static Server server;

    private ConfigUiExamples() {}

    public static void register() {
        client = ConfigManager.register(AnvilLibTest.MOD_ID, Client::new);
        ConfigManager.register(AnvilLibTest.MOD_ID, Legacy::new);
        server = ConfigManager.register(AnvilLibTest.MOD_ID, Server::new);
    }

    @Config(name = AnvilLibTest.MOD_ID, group = "anvillib", type = ModConfig.Type.CLIENT,
        screenStyle = ConfigScreenStyle.SIDEBAR)
    public static class Client {
        public boolean showHints = true;
        public String greeting = "Welcome";
        @BoundedDiscrete(min = 0.5, max = 4)
        @Slider(step = 0.05)
        public double previewScale = 1.37;
        @BoundedDiscrete(min = 0, max = 1)
        @Slider(step = 0.1)
        public float previewFraction = 0.3f;
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

    @Config(name = AnvilLibTest.MOD_ID, group = "anvillib", type = ModConfig.Type.SYNCED,
        screenStyle = ConfigScreenStyle.SIDEBAR)
    public static class Server {
        public boolean allowEditing = true;
        @BoundedDiscrete(min = 0.5, max = 4)
        @Slider(step = 0.05)
        public double previewScale = 1.37;
    }
}
