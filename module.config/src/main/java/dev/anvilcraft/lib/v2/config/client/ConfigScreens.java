package dev.anvilcraft.lib.v2.config.client;

import dev.anvilcraft.lib.v2.config.ConfigScreenStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** 仅从物理客户端调用，按 spec 身份选择编辑页。 */
public final class ConfigScreens {
    private ConfigScreens() {}

    public static void register(ModContainer mod, Function<ModConfigSpec, ConfigScreenStyle> style) {
        mod.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> create(container, parent, style));
    }

    public static Screen create(ModContainer mod, Screen parent, Function<ModConfigSpec, ConfigScreenStyle> style) {
        List<ModConfig> configs = Arrays.stream(ModConfig.Type.values())
            .flatMap(type -> ModConfigs.getConfigSet(type).stream())
            .filter(config -> config.getModId().equals(mod.getModId()))
            .sorted(Comparator.comparing(ModConfig::getType).thenComparing(ModConfig::getFileName)).toList();
        List<ModConfig> sidebar = configs.stream()
            .filter(config -> config.getSpec() instanceof ModConfigSpec spec && style.apply(spec) == ConfigScreenStyle.SIDEBAR)
            .toList();
        ConfigurationScreen selector = new ConfigurationScreen(mod, parent, (screen, type, config, title) ->
            sidebar.contains(config)
                ? new SidebarConfigScreen(screen, type, config, title, sidebar, false)
                : new ConfigurationScreen.ConfigurationSectionScreen(screen, type, config, title));
        List<ModConfig> editable = configs.stream().filter(config -> unavailableReason(config) == null).toList();
        if (!editable.isEmpty() && sidebar.containsAll(editable)) {
            ModConfig config = editable.stream().filter(candidate -> candidate.getType() == ModConfig.Type.CLIENT)
                .findFirst().orElse(editable.getFirst());
            return new SidebarConfigScreen(selector, config.getType(), config,
                selector.translatableConfig(config, ".title", "neoforge.configuration.uitext.title." + config.getType().extension()),
                sidebar, true);
        }
        return selector;
    }

    /** 与 NeoForge 文件选择页使用相同的加载与服务端配置限制。 */
    public static @Nullable Component unavailableReason(ModConfig config) {
        Minecraft minecraft = Minecraft.getInstance();
        return unavailableReason(config, minecraft.getCurrentServer() != null && !minecraft.isLocalServer(),
            minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer().isPublished());
    }

    static @Nullable Component unavailableReason(ModConfig config, boolean remote, boolean lan) {
        if (!(config.getSpec() instanceof ModConfigSpec spec) || !spec.isLoaded()) {
            return ConfigurationScreen.TOOLTIP_CANNOT_EDIT_NOT_LOADED;
        }
        if (config.getType() == ModConfig.Type.SERVER) {
            if (remote) return ConfigurationScreen.TOOLTIP_CANNOT_EDIT_THIS_WHILE_ONLINE;
            if (lan) return ConfigurationScreen.TOOLTIP_CANNOT_EDIT_THIS_WHILE_OPEN_TO_LAN;
        }
        return null;
    }
}
