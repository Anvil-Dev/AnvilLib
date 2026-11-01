package dev.anvilcraft.lib.v2.test.config;

import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.config.ConfigScreenStyle;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.AnvilLibTestConfig;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;

/** 在专用 GameTest 服务端验证启用了侧栏风格的配置仍可正常注册和加载。 */
@GameTestHolder(AnvilLibTest.MOD_ID)
public final class ConfigServerSmoke {
    @GameTestGenerator
    public static List<TestFunction> generateTests() {
        if (!Boolean.getBoolean("anvillib.configServerTest")) return List.of();
        return List.of(new TestFunction("config_smoke", AnvilLibTest.MOD_ID + ":config_server_registration",
            AnvilLibTest.MOD_ID + ":recipe_empty", 100, 0, true, ConfigServerSmoke::verify));
    }

    private static void verify(GameTestHelper helper) {
        if (AnvilLibTestConfig.class.getAnnotation(Config.class).screenStyle() != ConfigScreenStyle.SIDEBAR) {
            throw new AssertionError("Sidebar annotation did not survive server loading");
        }
        for (ModConfig config : ModConfigs.getModConfigs(AnvilLibTest.MOD_ID)) {
            if (config.getType() != ModConfig.Type.CLIENT && !((ModConfigSpec) config.getSpec()).isLoaded()) {
                throw new AssertionError("Server configuration not loaded: " + config.getFileName());
            }
        }
        helper.succeed();
    }
}
