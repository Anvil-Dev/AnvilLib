package dev.anvilcraft.lib.v2.test.config;

import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.config.ConfigScreenStyle;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.AnvilLibTestConfig;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** 在专用 GameTest 服务端验证启用了侧栏风格的配置仍可正常注册和加载。 */
@EventBusSubscriber(modid = AnvilLibTest.MOD_ID)
public final class ConfigServerSmoke {
    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        if (!Boolean.getBoolean("anvillib.configServerTest")) return;
        event.register(Registries.TEST_FUNCTION, registry ->
            registry.register(AnvilLibTest.of("config_server_registration"), ConfigServerSmoke::verify));
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        if (!Boolean.getBoolean("anvillib.configServerTest")) return;
        var environment = event.registerEnvironment(AnvilLibTest.of("config_smoke"));
        event.registerTest(AnvilLibTest.of("config_server_registration"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilLibTest.of("config_server_registration")),
            new TestData<>(environment, AnvilLibTest.of("recipe_empty"), 100, 0, true)));
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
