package dev.anvilcraft.lib.v2.config.client;

import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConfigAccessTest {
    @Test
    void unloadedRemoteAndLanServerConfigsAreDisabled() {
        var builder = new ModConfigSpec.Builder();
        builder.define("enabled", true);
        var tracker = new ConfigTracker();
        var mod = ModList.get().getModContainerById("anvillib_config").orElseThrow();
        var config = tracker.registerConfig(ModConfig.Type.SYNCED, builder.build(), mod, "access-test.toml");
        assertEquals(ConfigurationScreen.TOOLTIP_CANNOT_EDIT_NOT_LOADED, ConfigScreens.unavailableReason(config, false, false));
        tracker.loadDefaultSyncedConfigs();
        assertNull(ConfigScreens.unavailableReason(config, false, false));
        assertEquals(ConfigurationScreen.TOOLTIP_CANNOT_EDIT_THIS_WHILE_ONLINE, ConfigScreens.unavailableReason(config, true, false));
        assertEquals(ConfigurationScreen.TOOLTIP_CANNOT_EDIT_THIS_WHILE_OPEN_TO_LAN, ConfigScreens.unavailableReason(config, false, true));
    }

    @Test
    void savingUsesNeoForgeReloadNotification() {
        var builder = new ModConfigSpec.Builder();
        var value = builder.defineInRange("count", 2, 0, 10);
        var spec = builder.build();
        var tracker = new ConfigTracker();
        var mod = ModList.get().getModContainerById("anvillib_config").orElseThrow();
        var config = tracker.registerConfig(ModConfig.Type.SYNCED, spec, mod, "save-test.toml");
        tracker.loadDefaultSyncedConfigs();
        AtomicInteger reloads = new AtomicInteger();
        mod.getEventBus().addListener((ModConfigEvent.Reloading event) -> {
            if (event.getConfig() == config) reloads.incrementAndGet();
        });
        value.set(7);
        assertEquals(0, reloads.get());
        spec.save();
        assertEquals(1, reloads.get());
        assertEquals(7, value.get());
        ModConfigSpec.ValueSpec valueSpec = spec.getSpec().get("count");
        assertFalse(valueSpec.test(11));
        assertTrue(valueSpec.test(5));
    }
}
