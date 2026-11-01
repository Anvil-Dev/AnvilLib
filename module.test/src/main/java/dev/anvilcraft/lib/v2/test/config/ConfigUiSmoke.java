package dev.anvilcraft.lib.v2.test.config;

import com.mojang.datafixers.util.Function4;
import dev.anvilcraft.lib.v2.config.client.SidebarConfigScreen;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.AnvilLibTestConfig.TestEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 独立客户端中的交互回归；通过 -PconfigUiTest 运行并保留验收截图。 */
@EventBusSubscriber(modid = AnvilLibTest.MOD_ID, value = Dist.CLIENT)
public final class ConfigUiSmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUiSmoke.class);
    private static final KeyEvent ENTER = new KeyEvent(257, 0, 0);
    private static final KeyEvent ESCAPE = new KeyEvent(256, 0, 0);
    private static int ticks;
    private static int stage;
    private static int checks;
    private static boolean finished;
    private static SidebarConfigScreen editor;
    private static CompletableFuture<Void> reload;
    private static ConfigurationScreen restartParent;
    private static AbstractWidget detailsButton;
    private static AbstractWidget colorsButton;
    private static int enumStage;
    private static int nestedEnumStage;
    private static int compactStage;
    private static long marqueeTime;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Boolean.getBoolean("anvillib.configUiTest") || finished || minecraft.getOverlay() != null
            || minecraft.screen == null || ++ticks % 10 != 0) return;
        try {
            runStage(minecraft);
        } catch (Throwable error) {
            finished = true;
            LOGGER.error("CONFIG_UI_SMOKE_FAILED stage={}", stage, error);
            try {
                Files.writeString(minecraft.gameDirectory.toPath().resolve("config-ui-result.txt"), "FAIL " + stage + " " + error);
            } catch (Exception ignored) { }
            minecraft.stop();
        }
    }

    @SuppressWarnings("unchecked")
    private static void runStage(Minecraft minecraft) throws Exception {
        LOGGER.info("CONFIG_UI_SMOKE stage={}", stage);
        switch (stage) {
            case 0 -> {
                if (!minecraft.getLanguageManager().getSelected().equals("en_us")) {
                    minecraft.getLanguageManager().setSelected("en_us");
                    minecraft.options.languageCode = "en_us";
                    minecraft.reloadResourcePacks();
                    return;
                }
                var mod = ModList.get().getModContainerById(AnvilLibTest.MOD_ID).orElseThrow();
                var factory = mod.getCustomExtension(IConfigScreenFactory.class).orElseThrow();
                Screen screen = factory.createScreen(mod, minecraft.screen);
                check(screen instanceof ConfigurationScreen, "multiple files use NeoForge selector");
                var selector = (ConfigurationScreen) screen;
                var create = (Function4<ConfigurationScreen, ModConfig.Type, ModConfig, Component, Screen>) field(selector, "sectionScreen");
                ModConfig common = file(ModConfig.Type.COMMON, false);
                ModConfig legacy = file(ModConfig.Type.COMMON, true);
                List<ModConfig> others = ModConfigs.getModConfigs(AnvilLibTest.MOD_ID).stream()
                    .filter(config -> config != common).toList();
                try {
                    others.forEach(config -> ((ModConfigSpec) config.getSpec()).acceptConfig(null));
                    check(factory.createScreen(mod, minecraft.screen) instanceof SidebarConfigScreen,
                        "one editable sidebar file opens directly");
                } finally {
                    others.forEach(config -> ((ModConfigSpec) config.getSpec()).acceptConfig(config.getLoadedConfig()));
                }
                Screen legacyScreen = create.apply(selector, legacy.getType(), legacy, Component.literal("Legacy"));
                check(legacyScreen instanceof ConfigurationScreen.ConfigurationSectionScreen
                    && !(legacyScreen instanceof SidebarConfigScreen), "default style remains available");
                editor = (SidebarConfigScreen) create.apply(selector, common.getType(), common, Component.literal("AnvilLib Configuration"));
                minecraft.options.guiScale().set(2);
                minecraft.resizeDisplay();
                minecraft.setScreen(editor);
                check(((List<?>) field(editor, "tabs")).size() == 10, "general and all root groups become tabs");
            }
            case 1 -> {
                if (!checkEnumMenu(minecraft)) return;
                screenshot(minecraft, "config-sidebar.png");
                EditBox box = (EditBox) control(editor, "Test String");
                box.setValue("sidebar-regression");
                check("sidebar-regression".equals(value("test_string")), "valid text edits update spec");
                EditBox number = (EditBox) control(editor, "Test Int");
                number.setValue("77");
                number.setValue("-");
                check(Integer.valueOf(77).equals(value("test_int")), "partial number does not replace valid value");
                number.setValue("77");
                press(editor, ConfigurationScreen.UNDO.getString());
                check(Integer.valueOf(42).equals(value("test_int")), "undo restores last valid value");
                press(editor, ConfigurationScreen.RESET.getString());
                check("test".equals(value("test_string")), "reset restores direct values");
                ((EditBox) control(editor, "Test String")).setValue("saved-on-switch");
                press(editor, "Display");
            }
            case 2 -> {
                Object previous = value("display.opacity");
                AbstractWidget slider = control(editor, "Opacity");
                double targetX = ((Number) previous).intValue() == 100 ? slider.getX() + 5 : slider.getRight() - 5;
                var click = new MouseButtonEvent(targetX, slider.getY() + slider.getHeight() / 2.0, new MouseButtonInfo(0, 0));
                editor.mouseClicked(click, false);
                editor.mouseReleased(click);
                detailsButton = control(editor, "Details");
                pressControl(editor, "Details");
                check(!previous.equals(value("display.opacity")), "opening a modal commits pending slider input");
                check(((List<?>) field(editor, "modalPath")).size() == 1, "nested group opens a modal");
                check(editor.children().contains(editor.getFocused()), "modal focus belongs to current widgets");
            }
            case 3 -> {
                screenshot(minecraft, "config-modal-details.png");
                colorsButton = control(editor, "Colors");
                pressControl(editor, "Colors");
                check(((List<?>) field(editor, "modalPath")).size() == 2, "deeper group reuses modal path");
            }
            case 4 -> {
                if (nestedEnumStage == 0) {
                    pressControl(editor, "Mode");
                    check(field(editor, "dropdownMenu") != null, "enum dropdown opens inside a group modal");
                    nestedEnumStage++;
                    return;
                }
                if (nestedEnumStage == 1) {
                    screenshot(minecraft, "config-enum-modal.png");
                    editor.keyPressed(ESCAPE);
                    check(field(editor, "dropdownMenu") == null && ((List<?>) field(editor, "modalPath")).size() == 2,
                        "escape closes dropdown without closing the group modal");
                    nestedEnumStage++;
                    return;
                }
                screenshot(minecraft, "config-modal.png");
                int tab = (int) field(editor, "selectedTab");
                editor.mouseClicked(new MouseButtonEvent(20, 58, new MouseButtonInfo(0, 0)), false);
                check((int) field(editor, "selectedTab") == tab, "modal blocks background navigation");
                editor.keyPressed(new KeyEvent(258, 0, 0));
                check(editor.children().contains(editor.getFocused()), "keyboard tab focus stays inside modal");
                press(editor, Component.translatable("anvillib_config.ui.back").getString());
                check(((List<?>) field(editor, "modalPath")).size() == 1, "back button returns one modal level");
                check(colorsButton.isFocused(), "modal back restores the parent group button focus");
                editor.keyPressed(ESCAPE);
                check(((List<?>) field(editor, "modalPath")).isEmpty(), "escape restores main page");
                check(detailsButton.isFocused(), "closing modal restores the root group button focus");
                press(editor, "General");
                check("saved-on-switch".equals(((EditBox) control(editor, "Test String")).getValue()), "tab switch retains text");
                var panel = (net.minecraft.client.gui.components.OptionsList) field(activePage(editor), "list");
                panel.setScrollAmount(45);
                double scroll = panel.scrollAmount();
                press(editor, "Display");
                press(editor, "General");
                check(panel.scrollAmount() == scroll, "tab switch restores scroll");
                pressFileMenu(editor);
            }
            case 5 -> {
                screenshot(minecraft, "config-files.png");
                OptionsList menu = (OptionsList) editor.children().getFirst();
                AbstractWidget anchor = (AbstractWidget) field(editor, "fileButton");
                check(menu.getX() == anchor.getX() && menu.getY() == anchor.getBottom() + 2 && menu.getWidth() == anchor.getWidth(),
                    "file dropdown stays attached to its header control");
                AbstractWidget server = widget(editor, file(ModConfig.Type.SERVER, false).getFileName());
                check(!server.active, "unloaded server file is disabled");
                press(editor, file(ModConfig.Type.CLIENT, false).getFileName());
                check("saved-on-switch".equals(AnvilLibTest.CONFIG.testString), "file switch saves and reloads config object");
                check(AnvilLibTest.CONFIG.display.opacity == ((Number) value("display.opacity")).intValue(),
                    "nested slider edit reaches the registered configuration object");
                editor = (SidebarConfigScreen) minecraft.screen;
            }
            case 6 -> {
                pressFileMenu(editor);
                press(editor, file(ModConfig.Type.COMMON, false).getFileName());
                editor = (SidebarConfigScreen) minecraft.screen;
                check(!widget(editor, ConfigurationScreen.UNDO.getString()).active, "save clears file undo history");
                minecraft.options.guiScale().set(3);
                minecraft.resizeDisplay();
            }
            case 7 -> {
                if (compactStage == 0) {
                    OptionsList navigation = (OptionsList) field(editor, "navigation");
                    check(navigation.getBottom() == editor.height, "sidebar scroll area reaches the bottom of the window");
                    exerciseScrollbar(navigation, "sidebar");
                    exerciseScrollbar((OptionsList) field(activePage(editor), "list"), "stacked content");
                    navigation.setScrollAmount(navigation.maxScrollAmount());
                    press(editor, "Interface Options");
                    marqueeTime = System.nanoTime();
                    compactStage++;
                    return;
                }
                if (compactStage == 1) {
                    if (System.nanoTime() - marqueeTime < 2_000_000_000L) return;
                    screenshot(minecraft, "config-sidebar-scrolling.png");
                    press(editor, "General");
                    ((OptionsList) field(editor, "navigation")).setScrollAmount(0);
                    compactStage++;
                    return;
                }
                screenshot(minecraft, "config-compact.png");
                check(editor.width < 500, "compact layout exercised");
                minecraft.getLanguageManager().setSelected("zh_cn");
                minecraft.options.languageCode = "zh_cn";
                reload = minecraft.reloadResourcePacks();
            }
            case 8 -> {
                if (!reload.isDone()) return;
                reload.join();
                editor.resize(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
            }
            case 9 -> {
                screenshot(minecraft, "config-chinese.png");
                check(widget(editor, "常规") != null, "Chinese UI translation loaded");
                editor.keyPressed(ESCAPE);
                check(minecraft.screen instanceof ConfigurationScreen, "done returns to file selector");
                restartParent = (ConfigurationScreen) minecraft.screen;
                var builder = new ModConfigSpec.Builder();
                builder.translation("gui.done").define("restart_check", false);
                var mod = ModList.get().getModContainerById(AnvilLibTest.MOD_ID).orElseThrow();
                var config = ConfigTracker.INSTANCE.registerConfig(ModConfig.Type.STARTUP, builder.build(), mod, "config-ui-restart-test.toml");
                editor = new SidebarConfigScreen(restartParent, config.getType(), config, Component.literal("Restart verification"));
                minecraft.setScreen(editor);
            }
            case 10 -> pressControl(editor, Component.translatable("gui.done").getString());
            case 11 -> {
                editor.onClose();
                check(restartParent.needsRestart == ModConfigSpec.RestartType.GAME, "startup edits propagate restart requirement");
                restartParent.onClose();
                check(minecraft.screen.getTitle().getString().equals(ConfigurationScreen.GAME_RESTART_TITLE.getString()),
                    "NeoForge shows the restart confirmation");
                Files.writeString(minecraft.gameDirectory.toPath().resolve("config-ui-result.txt"), "PASS " + checks);
                LOGGER.info("CONFIG_UI_SMOKE_PASSED checks={}", checks);
                finished = true;
            }
            default -> throw new IllegalStateException("Unexpected stage " + stage);
        }
        stage++;
        if (finished) minecraft.execute(minecraft::stop);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        LOGGER.info("Config UI check: {}", message);
    }

    private static boolean checkEnumMenu(Minecraft minecraft) throws Exception {
        switch (enumStage++) {
            case 0 -> {
                OptionsList navigation = (OptionsList) field(editor, "navigation");
                AbstractWidget first = (AbstractWidget) ((ContainerEventHandler) navigation.children().getFirst()).children().getFirst();
                AbstractWidget second = (AbstractWidget) ((ContainerEventHandler) navigation.children().get(1)).children().getFirst();
                check(second.getY() - first.getBottom() == 1, "sidebar options have a one-pixel gap");
                check(navigation.getBottom() == editor.height, "wide sidebar reaches the window bottom");
                exerciseScrollbar((OptionsList) field(activePage(editor), "list"), "content");
                press(editor, ConfigurationScreen.RESET.getString());
                ((OptionsList) field(activePage(editor), "list")).setScrollAmount(Double.MAX_VALUE);
            }
            case 1 -> {
                AbstractWidget control = control(editor, "Test Enum");
                check(control instanceof AbstractButton && !(control instanceof CycleButton<?>), "enum uses a dropdown instead of a cycle button");
                pressControl(editor, "Test Enum");
                check(field(editor, "dropdownMenu") != null && value("test_enum") == TestEnum.TEST, "opening dropdown does not change the value");
                check(widget(editor, TestEnum.TEST.getTranslatedName().getString()).isFocused(), "dropdown focuses the current value");
            }
            case 2 -> {
                screenshot(minecraft, "config-enum-dropdown.png");
                var menu = (OptionsList) editor.children().getFirst();
                check(menu.getBottom() < 300, "dropdown opens upwards near the bottom of the window");
                exerciseScrollbar(menu, "dropdown");
                click(editor, widget(editor, TestEnum.TEST2.getTranslatedName().getString()));
                check(value("test_enum") == TestEnum.TEST2 && field(editor, "dropdownMenu") == null, "mouse selection commits and closes dropdown");
                press(editor, ConfigurationScreen.UNDO.getString());
                check(value("test_enum") == TestEnum.TEST, "enum selection can be undone");
            }
            case 3 -> {
                pressControl(editor, "Test Enum");
                editor.keyPressed(new KeyEvent(264, 0, 0));
                editor.keyPressed(ENTER);
                check(value("test_enum") == TestEnum.TEST2 && field(editor, "dropdownMenu") == null, "arrow keys and enter select an enum value");
                press(editor, ConfigurationScreen.RESET.getString());
                check(value("test_enum") == TestEnum.TEST, "reset restores the default enum value");
            }
            case 4 -> {
                pressControl(editor, "Test Enum");
                editor.keyPressed(ESCAPE);
                check(minecraft.screen == editor && field(editor, "dropdownMenu") == null && value("test_enum") == TestEnum.TEST,
                    "escape cancels dropdown without leaving the configuration");
                check(control(editor, "Test Enum").isFocused(), "closing dropdown restores enum control focus");
                pressControl(editor, "Test Enum");
                int tab = (int) field(editor, "selectedTab");
                editor.mouseClicked(new MouseButtonEvent(20, 90, new MouseButtonInfo(0, 0)), false);
                check(field(editor, "dropdownMenu") == null && (int) field(editor, "selectedTab") == tab,
                    "outside click closes dropdown without activating the background");
                ((OptionsList) field(activePage(editor), "list")).setScrollAmount(0);
            }
            default -> { return true; }
        }
        return false;
    }

    private static ModConfig file(ModConfig.Type type, boolean legacy) {
        return ModConfigs.getModConfigs(AnvilLibTest.MOD_ID).stream()
            .filter(config -> config.getType() == type && config.getFileName().contains("legacy") == legacy)
            .findFirst().orElseThrow();
    }

    private static Object value(String path) {
        ModConfigSpec spec = (ModConfigSpec) file(ModConfig.Type.COMMON, false).getSpec();
        return ((ModConfigSpec.ConfigValue<?>) spec.getValues().get(path)).getRaw();
    }

    private static void pressFileMenu(Screen screen) throws ReflectiveOperationException {
        ((AbstractButton) field(screen, "fileButton")).onPress(ENTER);
    }

    private static void exerciseScrollbar(OptionsList panel, String name) {
        check(panel.maxScrollAmount() > 0, name + " has overflowing entries");
        panel.setScrollAmount(0);
        double x = panel.getRight() - 6 / 2.0;
        double fromY = panel.getY() + 3;
        double toY = panel.getBottom() - 2;
        MouseButtonEvent press = new MouseButtonEvent(x, fromY, new MouseButtonInfo(0, 0));
        check(editor.mouseClicked(press, false), name + " scrollbar receives mouse clicks");
        MouseButtonEvent drag = new MouseButtonEvent(x, toY, new MouseButtonInfo(0, 0));
        editor.mouseDragged(drag, 0, toY - fromY);
        editor.mouseReleased(drag);
        check(panel.scrollAmount() > 0, name + " scrollbar thumb drags");
        panel.setScrollAmount(0);
        editor.mouseClicked(drag, false);
        editor.mouseReleased(drag);
        check(panel.scrollAmount() > 0, name + " scrollbar track jumps to the clicked position");
        panel.setScrollAmount(0);
    }

    private static void press(Screen screen, String label) {
        ((AbstractButton) widget(screen, label)).onPress(ENTER);
    }

    private static void pressControl(Screen screen, String label) {
        click(screen, control(screen, label));
    }

    private static void click(Screen screen, AbstractWidget widget) {
        check(screen.mouseClicked(new MouseButtonEvent(widget.getX() + widget.getWidth() / 2.0,
            widget.getY() + widget.getHeight() / 2.0, new MouseButtonInfo(0, 0)), false), "control responds to mouse input");
    }

    private static AbstractWidget widget(Screen screen, String label) {
        return all(screen).stream().filter(node -> node instanceof AbstractWidget widget && widget.getMessage().getString().equals(label))
            .map(node -> (AbstractWidget) node).findFirst()
            .orElseThrow(() -> new AssertionError("Missing widget: " + label + " available=" + all(screen).stream()
                .filter(node -> node instanceof AbstractWidget).map(node -> ((AbstractWidget) node).getMessage().getString()).toList()));
    }

    private static AbstractWidget control(Screen screen, String label) {
        for (GuiEventListener node : all(screen)) {
            if (node instanceof ContainerEventHandler container && container.children().size() == 2
                && container.children().getFirst() instanceof AbstractWidget name && name.getMessage().getString().equals(label)) {
                return (AbstractWidget) container.children().get(1);
            }
        }
        throw new AssertionError("Missing configuration row: " + label);
    }

    private static List<GuiEventListener> all(ContainerEventHandler container) {
        List<GuiEventListener> result = new ArrayList<>();
        for (GuiEventListener node : container.children()) {
            result.add(node);
            if (node instanceof ContainerEventHandler child) result.addAll(all(child));
        }
        return result;
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object activePage(SidebarConfigScreen screen) throws ReflectiveOperationException {
        var method = SidebarConfigScreen.class.getDeclaredMethod("activePage");
        method.setAccessible(true);
        return method.invoke(screen);
    }

    private static void screenshot(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(), 1,
            message -> LOGGER.info("Config UI screenshot: {}", message.getString()));
    }
}
