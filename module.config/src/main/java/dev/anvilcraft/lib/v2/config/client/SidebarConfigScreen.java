package dev.anvilcraft.lib.v2.config.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.RestartType;
import net.neoforged.neoforge.common.TranslatableEnum;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 按配置文件显示侧栏。值控件、验证和撤销由 NeoForge 提供，
 * 页面切换只改变可交互控件；文件切换或退出时才保存。
 */
public class SidebarConfigScreen extends ConfigurationScreen.ConfigurationSectionScreen {
    private static final String LANG = "anvillib_config.ui.";
    private final Session session;
    private final List<ConfigTabs.Tab> tabs;
    private final Map<String, SectionPage> pages = new HashMap<>();
    private final Map<String, Double> scrollPositions = new HashMap<>();
    private final List<SectionPage> modalPath = new ArrayList<>();
    private @Nullable PanelOptionsList navigation;
    private @Nullable FlatButton fileButton;
    private @Nullable FlatButton undo;
    private @Nullable FlatButton reset;
    private @Nullable DropdownMenu dropdownMenu;
    private int selectedTab;
    private double navigationScroll;

    private record DropdownMenu(FlatButton anchor, PanelOptionsList panel, List<Renderable> background,
                                @Nullable AbstractWidget selected) {}

    private static final class Session {
        final ConfigurationScreen selector;
        final List<ModConfig> files;
        final boolean direct;
        final Map<ModConfig, SidebarConfigScreen> screens = new IdentityHashMap<>();

        Session(ConfigurationScreen selector, List<ModConfig> files, boolean direct) {
            this.selector = selector;
            this.files = List.copyOf(files);
            this.direct = direct;
        }
    }

    /** 手动集成时可使用；通过 @Config 启用时由 ConfigManager 自动构造。 */
    public SidebarConfigScreen(ConfigurationScreen parent, ModConfig.Type type, ModConfig config, Component title) {
        this(parent, type, config, title, List.of(config), false);
    }

    SidebarConfigScreen(ConfigurationScreen parent, ModConfig.Type type, ModConfig config, Component title,
                        List<ModConfig> files, boolean direct) {
        this(new Session(parent, files, direct), type, config, title);
    }

    private SidebarConfigScreen(Session session, ModConfig.Type type, ModConfig config, Component title) {
        super(session.selector, type, config, title);
        this.session = session;
        this.tabs = ConfigTabs.of(context.modSpec());
        session.screens.put(config, this);
    }

    private static Component text(String key) {
        return Component.translatable(LANG + key);
    }

    @Override
    protected void init() {
        refreshLayout();
    }

    @Override
    protected void repositionElements() {
        refreshLayout();
    }

    @Override
    protected void addOptions() {}

    private Component tabTitle(ConfigTabs.Tab tab) {
        return tab.key().isEmpty() ? text("general") : getTranslationComponent(tab.key());
    }

    private @Nullable SectionPage tabPage() {
        if (tabs.isEmpty()) return null;
        ConfigTabs.Tab tab = tabs.get(selectedTab);
        return pages.computeIfAbsent(tab.key(), key -> {
            Context tabContext = key.isEmpty()
                ? new Context(context.modId(), this, context.modConfig(), context.modSpec(),
                    tab.entries(), tab.specs(), List.of(), context.filter())
                : Context.section(context, this, tab.entries(), tab.specs(), key);
            return new SectionPage(tabContext, tabTitle(tab));
        });
    }

    private @Nullable SectionPage activePage() {
        return modalPath.isEmpty() ? tabPage() : modalPath.getLast();
    }

    private int sidebarWidth() {
        return Math.clamp(width / 4, 82, 170);
    }

    private int modalWidth() {
        return Math.min(560, width - 32);
    }

    private int modalHeight() {
        SectionPage page = activePage();
        int rows = page == null ? 0 : page.entryCount();
        int rowHeight = modalWidth() - 24 < 330 ? 50 : 32;
        return Math.min(height - 48, Math.max(150, rows * rowHeight + 88));
    }

    private void refreshLayout() {
        if (dropdownMenu != null) dropdownMenu.anchor().expanded = false;
        dropdownMenu = null;
        fileButton = null;
        if (navigation != null) navigationScroll = navigation.getScrollAmount();
        for (SectionPage page : pages.values()) {
            if (page.prepared && getFocused() == page.panel()) page.rememberFocus();
        }
        clearWidgets();
        setFocused(null);
        int sidebar = sidebarWidth();
        navigation = new PanelOptionsList(this);
        for (int i = 0; i < tabs.size(); i++) {
            int index = i;
            FlatButton button = new FlatButton(tabTitle(tabs.get(i)), () -> selectTab(index));
            button.selected = index == selectedTab;
            navigation.addSmall(button, (AbstractWidget) null);
        }
        navigation.bounds(0, 42, sidebar, Math.max(20, height - 42));
        navigation.setScrollAmount(navigationScroll);
        SectionPage base = tabPage();
        if (base != null) base.prepare(sidebar + 8, 48, Math.max(60, width - sidebar - 20), Math.max(20, height - 96));
        SectionPage page = activePage();
        int left = sidebar + 8;
        int right = width - 12;
        int footer = height - 36;
        if (!modalPath.isEmpty()) {
            int panelWidth = modalWidth();
            int panelHeight = modalHeight();
            int panelY = (height - panelHeight) / 2;
            left = (width - panelWidth) / 2 + 12;
            right = (width + panelWidth) / 2 - 12;
            footer = panelY + panelHeight - 36;
            if (page != null) page.prepare(left, panelY + 40, right - left, Math.max(20, panelHeight - 84));
            StringWidget title = new StringWidget(left, panelY + 8, right - left, 24, page.getTitle(), font);
            title.setTooltip(Tooltip.create(page.getTitle()));
            addRenderableWidget(title);
        } else {
            addRenderableWidget(navigation);
            if (session.files.size() > 1) {
                fileButton = new FlatButton(Component.literal(context.modConfig().getFileName()), anchor -> openFileMenu((FlatButton) anchor));
                fileButton.dropdown = true;
                fileButton.setRectangle(Math.min(210, width / 2), 24, width - Math.min(210, width / 2) - 12, 12);
                addRenderableWidget(fileButton);
            }
        }
        if (page != null) {
            addRenderableWidget(page.panel());
            undo = new FlatButton(ConfigurationScreen.UNDO, page::undo);
            reset = new FlatButton(ConfigurationScreen.RESET, page::reset);
            undo.active = page.canUndo();
            reset.active = page.canReset();
            int buttonWidth = Math.min(112, Math.max(40, (right - left - 16) / 3));
            undo.setRectangle(buttonWidth, 24, left, footer);
            reset.setRectangle(buttonWidth, 24, left + buttonWidth + 8, footer);
            addRenderableWidget(undo);
            addRenderableWidget(reset);
            setFocused(page.panel());
            page.restoreFocus();
        } else {
            undo = null;
            reset = null;
        }
        FlatButton done = new FlatButton(modalPath.isEmpty() ? CommonComponents.GUI_DONE : text("back"), this::onClose);
        done.setRectangle(Math.min(112, Math.max(40, (right - left - 16) / 3)), 24,
            right - Math.min(112, Math.max(40, (right - left - 16) / 3)), footer);
        addRenderableWidget(done);
    }

    private void openFileMenu(FlatButton anchor) {
        PanelOptionsList panel = new PanelOptionsList(this);
        AbstractWidget selected = null;
        for (ModConfig file : session.files) {
            FlatButton button = new FlatButton(Component.literal(file.getFileName()), () -> {
                closeDropdown();
                switchFile(file);
            });
            button.selected = file == context.modConfig();
            if (button.selected) selected = button;
            Component reason = ConfigScreens.unavailableReason(file);
            button.active = reason == null;
            if (reason != null) button.setTooltip(Tooltip.create(reason));
            panel.addSmall(button, (AbstractWidget) null);
        }
        showDropdown(anchor, panel, selected);
    }

    private void selectTab(int index) {
        flushActive();
        selectedTab = index;
        refreshLayout();
    }

    private void flushActive() {
        SectionPage page = activePage();
        if (page != null) page.panel().applyUnsavedChanges();
    }

    private static Component enumName(Enum<?> value) {
        return value instanceof TranslatableEnum translated ? translated.getTranslatedName() : Component.literal(value.name());
    }

    private <T extends Enum<T>> void openEnumMenu(FlatButton anchor, List<T> choices, T selected, Consumer<T> choose) {
        PanelOptionsList panel = new PanelOptionsList(this);
        AbstractWidget selectedButton = null;
        for (T choice : choices) {
            Component label = enumName(choice);
            FlatButton option = new FlatButton(label, () -> {
                choose.accept(choice);
                closeDropdown();
            });
            option.selected = choice == selected;
            if (option.selected) selectedButton = option;
            panel.addSmall(option, (AbstractWidget) null);
        }
        showDropdown(anchor, panel, selectedButton);
    }

    private void showDropdown(FlatButton anchor, PanelOptionsList panel, @Nullable AbstractWidget selected) {
        flushActive();
        SectionPage page = activePage();
        if (page != null && anchor != fileButton) {
            page.panel().focusWidget(anchor);
            page.rememberFocus();
        }
        int menuWidth = Math.min(anchor.getWidth(), width - 24);
        int below = height - anchor.getBottom() - 14;
        int above = anchor.getY() - 14;
        int desiredHeight = panel.preferredHeight();
        boolean down = below >= desiredHeight || below >= above;
        int menuHeight = Math.min(desiredHeight, Math.max(20, down ? below : above));
        int x = Math.clamp(anchor.getX(), 12, width - menuWidth - 12);
        int y = down ? anchor.getBottom() + 2 : anchor.getY() - menuHeight - 2;
        panel.bounds(x, y, menuWidth, menuHeight);
        anchor.expanded = true;
        dropdownMenu = new DropdownMenu(anchor, panel, renderablesForSidebar(), selected);
        clearWidgets();
        setFocused(null);
        addRenderableWidget(panel);
        setFocused(panel);
        if (selected != null) panel.focusWidget(selected);
    }

    private void closeDropdown() {
        boolean focusFile = dropdownMenu != null && dropdownMenu.anchor() == fileButton;
        refreshLayout();
        if (focusFile && fileButton != null) setFocused(fileButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dropdownMenu != null && !dropdownMenu.panel().isMouseOver(mouseX, mouseY)) {
            closeDropdown();
            return true;
        }
        SectionPage previous = activePage();
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // 按钮回调可能重建控件；父容器随后会把焦点设回已移除的按钮/列表。
        // 修正这个重入顺序，防止弹窗打开后键盘事件仍到达背景页。
        if (getFocused() != null && !children().contains(getFocused())) {
            if (previous != null && getFocused() == previous.panel()) previous.rememberFocus();
            SectionPage page = activePage();
            setFocused(dropdownMenu != null ? dropdownMenu.panel() : page == null ? navigation : page.panel());
            if (dropdownMenu != null && dropdownMenu.selected() != null) dropdownMenu.panel().focusWidget(dropdownMenu.selected());
            if (dropdownMenu == null && page != null) page.restoreFocus();
            setDragging(false);
        }
        return handled;
    }

    private void switchFile(ModConfig file) {
        if (ConfigScreens.unavailableReason(file) != null) return;
        if (file == context.modConfig()) {
            refreshLayout();
            return;
        }
        saveFile();
        for (Map.Entry<String, SectionPage> entry : pages.entrySet()) {
            scrollPositions.put(entry.getKey(), entry.getValue().panel().getScrollAmount());
        }
        pages.clear(); // 保存是撤销历史的边界，导航位置单独保留。
        SidebarConfigScreen next = session.screens.get(file);
        if (next == null) {
            next = new SidebarConfigScreen(session, file.getType(), file,
                session.selector.translatableConfig(file, ".title",
                    "neoforge.configuration.uitext.title." + file.getType().extension()));
        }
        minecraft.setScreen(next);
    }

    private void saveFile() {
        for (SectionPage page : pages.values()) page.panel().applyUnsavedChanges();
        if (changed && context.modSpec().isLoaded()) {
            context.modSpec().save();
            session.selector.needsRestart = session.selector.needsRestart.with(needsRestart);
            changed = false;
            needsRestart = context.modConfig().getType() == ModConfig.Type.STARTUP ? RestartType.GAME : RestartType.NONE;
        }
    }

    @Override
    public void onClose() {
        if (dropdownMenu != null) {
            closeDropdown();
        } else if (!modalPath.isEmpty()) {
            flushActive();
            modalPath.removeLast();
            refreshLayout();
        } else {
            saveFile();
            session.screens.clear();
            if (session.direct) session.selector.onClose();
            else minecraft.setScreen(session.selector);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF141922);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.fill(0, 0, width, height, 0xFF141922);
        graphics.fill(0, 42, sidebarWidth(), height, 0xFF1A202B);
        int titleWidth = session.files.size() > 1 ? Math.max(20, width / 2 - 30) : width - 24;
        graphics.drawString(font, ellipsis(getTitle().getString(), titleWidth), 12, 20, 0xFFE8EDF5, false);
        if (tabs.isEmpty()) graphics.drawString(font, text("empty"), sidebarWidth() + 16, 62, 0xFFABB7C8, false);
        if (!modalPath.isEmpty()) {
            if (navigation != null) navigation.render(graphics, -1, -1, partialTick);
            SectionPage base = tabPage();
            if (base != null) base.panel().render(graphics, -1, -1, partialTick);
            // 旧版字体和原版控件使用独立深度，弹层必须整体高于背景内容。
            graphics.pose().translate(0, 0, 200);
            graphics.fill(0, 0, width, height, 0xB0000000);
            int panelWidth = modalWidth();
            int panelHeight = modalHeight();
            int x = (width - panelWidth) / 2;
            int y = (height - panelHeight) / 2;
            graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xFF1A222E);
            graphics.fill(x, y, x + panelWidth, y + 36, 0xFF242E3D);
        }
        SectionPage page = activePage();
        if (undo != null && page != null) undo.active = page.canUndo();
        if (reset != null && page != null) reset.active = page.canReset();
        // 不调用 NeoForge 的页脚状态更新：此界面的按钮属于当前子页。
        if (dropdownMenu == null) {
            for (var renderable : renderablesForSidebar()) renderable.render(graphics, mouseX, mouseY, partialTick);
        } else {
            for (var renderable : dropdownMenu.background()) renderable.render(graphics, -1, -1, partialTick);
            graphics.pose().translate(0, 0, 200);
            PanelOptionsList panel = dropdownMenu.panel();
            graphics.fill(panel.getX() - 1, panel.getY() - 1, panel.getRight() + 1, panel.getBottom() + 1, 0xFF53637A);
            graphics.fill(panel.getX(), panel.getY(), panel.getRight(), panel.getBottom(), 0xFF1A222E);
            panel.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.pose().popPose();
    }

    private List<Renderable> renderablesForSidebar() {
        return children().stream().filter(child -> child instanceof Renderable)
            .map(child -> (Renderable) child).toList();
    }

    private String ellipsis(String value, int width) {
        return font.width(value) <= width ? value : font.plainSubstrByWidth(value, Math.max(0, width - 10)) + "…";
    }

    private final class SectionPage extends ConfigurationScreen.ConfigurationSectionScreen {
        private boolean prepared;
        private @Nullable ComponentPath savedFocus;

        SectionPage(Context context, Component title) {
            super(context, title);
        }

        PanelOptionsList panel() {
            return (PanelOptionsList) list;
        }

        void rememberFocus() {
            savedFocus = panel().getCurrentFocusPath();
        }

        void restoreFocus() {
            if (savedFocus != null) savedFocus.applyFocus(true);
        }

        int entryCount() {
            return context.entries().size();
        }

        void prepare(int x, int y, int width, int height) {
            if (!prepared) {
                this.minecraft = SidebarConfigScreen.this.minecraft;
                this.font = SidebarConfigScreen.this.font;
                this.width = SidebarConfigScreen.this.width;
                this.height = SidebarConfigScreen.this.height;
                list = new PanelOptionsList(this);
                rebuild();
                prepared = true;
                panel().bounds(x, y, width, height);
                panel().setScrollAmount(scrollPositions.getOrDefault(String.join(".", context.keylist()), 0.0));
            } else {
                panel().bounds(x, y, width, height);
            }
        }

        @Override
        protected ConfigurationScreen.ConfigurationSectionScreen rebuild() {
            // 旧版 NeoForge 直接清空 children，需要同时清除自定义行布局。
            if (list instanceof PanelOptionsList panel) panel.clearEntries();
            return super.rebuild();
        }

        @Override
        protected String getTranslationKey(String key) {
            ModConfigSpec.ValueSpec spec = getValueSpec(key);
            String translated = spec != null ? spec.getTranslationKey()
                : context.modSpec().getLevelTranslationKey(absolutePath(key));
            return translated != null ? translated : context.modId() + ".configuration." + String.join(".", absolutePath(key));
        }

        private List<String> absolutePath(String key) {
            List<String> result = new ArrayList<>(context.keylist());
            result.add(key);
            return result;
        }

        @Override
        protected void onChanged(String key) {
            super.onChanged(key);
            SidebarConfigScreen.this.changed = true;
            SidebarConfigScreen.this.needsRestart = SidebarConfigScreen.this.needsRestart.with(needsRestart);
        }

        @Override
        protected <T extends Enum<T>> Element createEnumValue(String key, ModConfigSpec.ValueSpec spec,
                                                              Supplier<T> source, Consumer<T> target) {
            List<T> choices = Arrays.stream(source.get().getDeclaringClass().getEnumConstants()).filter(spec::test).toList();
            Consumer<T> update = value -> {
                target.accept(value);
                onChanged(key);
            };
            FlatButton button = new FlatButton(enumName(source.get()), anchor ->
                openEnumMenu((FlatButton) anchor, choices, source.get(), value -> {
                    if (value != source.get()) undoManager.add(update, value, update, source.get());
                    anchor.setMessage(enumName(source.get()));
                }));
            button.dropdown = true;
            button.active = !choices.isEmpty();
            button.setTooltip(Tooltip.create(getTooltipComponent(key, null)));
            return new Element(getTranslationComponent(key), getTooltipComponent(key, null), button);
        }

        @Override
        protected @Nullable Element createSection(String key, UnmodifiableConfig specs, UnmodifiableConfig values) {
            if (specs.isEmpty()) return null;
            FlatButton open = new FlatButton(text("open"), () -> {
                flushActive();
                String path = String.join(".", absolutePath(key));
                SectionPage page = pages.computeIfAbsent(path, ignored ->
                    new SectionPage(Context.section(context, SidebarConfigScreen.this, values.entrySet(), specs.valueMap(), key),
                        getTranslationComponent(key)));
                modalPath.add(page);
                refreshLayout();
            });
            open.setTooltip(Tooltip.create(getTooltipComponent(key, null)));
            return new Element(getTranslationComponent(key), getTooltipComponent(key, null), open, false);
        }

        @Override
        protected <T> Element createList(String key, ModConfigSpec.ListValueSpec spec, ModConfigSpec.ConfigValue<List<T>> value) {
            FlatButton open = new FlatButton(text("open"), () -> {
                flushActive();
                // 保留 NeoForge 的列表增删、排序和列表级校验。
                minecraft.setScreen(new ConfigurationScreen.ConfigurationListScreen<T>(
                    Context.list(context, SidebarConfigScreen.this), key, getTranslationComponent(key), spec, value) {
                    @Override
                    public void onClose() {
                        if (changed && spec.test(cfgList)) SectionPage.this.onChanged(key);
                        super.onClose();
                    }
                });
            });
            return new Element(getTranslationComponent(key), getTooltipComponent(key, null), open, false);
        }

        boolean canUndo() {
            return undoManager.canUndo();
        }

        boolean canReset() {
            return isAnyNondefault();
        }

        void undo() {
            panel().applyUnsavedChanges();
            undoManager.undo();
            rebuildKeepingScroll();
        }

        void reset() {
            panel().applyUnsavedChanges();
            savedFocus = null;
            double scroll = panel().getScrollAmount();
            if (resetButton != null) resetButton.onPress();
            refreshLayout();
            panel().setScrollAmount(scroll);
        }

        private void rebuildKeepingScroll() {
            double scroll = panel().getScrollAmount();
            savedFocus = null;
            rebuild();
            refreshLayout();
            panel().setScrollAmount(scroll);
        }
    }
}
