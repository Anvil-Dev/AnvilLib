package dev.anvilcraft.lib.v2.test.wheel.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.test.AnvilLibTestConfig;
import dev.anvilcraft.lib.v2.test.config.ConfigUiExamples;

public class LangHandler {
    public static void init(RegistrumLangProvider provider) {
        WheelLang.init(provider);
        provider.add("key.category.anvillib_test.flexible", "AnvilLib Test-Flexible");
        provider.add("key.anvillib_test.flexible_screen", "Flexible Control Screen");
        ConfigData.readConfigClass(provider, AnvilLibTestConfig.class);
        ConfigData.readConfigClass(provider, ConfigUiExamples.Client.class);
        ConfigData.readConfigClass(provider, ConfigUiExamples.Legacy.class);
        ConfigData.readConfigClass(provider, ConfigUiExamples.Server.class);
    }
}
