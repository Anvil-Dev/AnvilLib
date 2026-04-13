package dev.anvilcraft.lib.v2.test.wheel.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class WheelLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("key.categories.anvillib_test.wheel", "AnvilLib Test-Wheel");
        provider.add("key.anvillib_test.wheel_tap", "Wheel Tap");
        provider.add("key.anvillib_test.wheel_hold", "Wheel Hold");
    }
}
