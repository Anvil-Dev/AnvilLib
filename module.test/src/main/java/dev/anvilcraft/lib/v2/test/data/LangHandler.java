package dev.anvilcraft.lib.v2.test.data;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class LangHandler {
    public static void addLang(RegistrumLangProvider provider) {
        provider.add("key.categories.anvillib_test.wheel", "AnvilLib Test-Wheel");
        provider.add("key.anvillib_test.wheel_tap", "Wheel Tap");
        provider.add("key.anvillib_test.wheel_hold", "Wheel Hold");
        provider.add("key.anvillib_test.wheel_tap_annular", "Wheel Tap (Annular)");
        provider.add("key.anvillib_test.wheel_hold_annular", "Wheel Hold (Annular)");
    }
}
