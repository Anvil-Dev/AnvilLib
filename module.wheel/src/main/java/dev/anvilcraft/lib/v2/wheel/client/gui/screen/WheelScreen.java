package dev.anvilcraft.lib.v2.wheel.client.gui.screen;

import dev.anvilcraft.lib.v2.wheel.api.WheelActionContext;
import dev.anvilcraft.lib.v2.wheel.api.WheelEntry;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.api.WheelOpenMode;
import dev.anvilcraft.lib.v2.wheel.api.WheelPageModel;
import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class WheelScreen extends Screen {
    private final WheelMenuModel model;
    private final WheelOpenMode openMode;
    private final Deque<List<WheelEntry>> menuStack = new ArrayDeque<>();

    private WheelWidget wheelWidget;
    private int currentPageIndex;

    public WheelScreen(WheelMenuModel model, WheelOpenMode openMode) {
        super(Component.empty());
        this.model = model;
        this.openMode = openMode;
        this.menuStack.addLast(this.model.rootEntries());
    }

    @Override
    protected void init() {
        this.rebuildWheelWidget();
    }

    public static WheelScreen tap(WheelMenuModel model) {
        return new WheelScreen(model, WheelOpenMode.TAP);
    }

    public static WheelScreen hold(WheelMenuModel model) {
        return new WheelScreen(model, WheelOpenMode.HOLD);
    }

    public void triggerFromHoldRelease() {
        if (this.openMode != WheelOpenMode.HOLD) {
            return;
        }
        this.triggerSelectedOrClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        if (this.wheelWidget != null) {
            this.wheelWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        Component pageInfo = Component.literal((this.currentPageIndex + 1) + " / " + this.pageCountForCurrentMenu());
        guiGraphics.drawCenteredString(this.font, pageInfo, this.width / 2, this.height - 22, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return false;
        }
        int totalPages = this.pageCountForCurrentMenu();
        if (totalPages <= 1) {
            return true;
        }
        if (scrollY > 0) {
            this.currentPageIndex = (this.currentPageIndex + 1) % totalPages;
        } else {
            this.currentPageIndex = (this.currentPageIndex - 1 + totalPages) % totalPages;
        }
        this.rebuildWheelWidget();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.openMode == WheelOpenMode.TAP && button == 0) {
            this.triggerSelectedOrClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (this.menuStack.size() > 1) {
                this.menuStack.removeLast();
                this.currentPageIndex = 0;
                this.rebuildWheelWidget();
            } else {
                this.onClose();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.wheelWidget != null) {
            this.wheelWidget.onClosing();
            return;
        }
        super.onClose();
    }

    private void triggerSelectedOrClose() {
        if (this.wheelWidget == null) {
            this.onClose();
            return;
        }
        int selectedIndex = this.wheelWidget.getCurrentSectionIndex();
        if (selectedIndex < 0) {
            this.onClose();
            return;
        }

        WheelPageModel page = this.currentPage();
        WheelEntry entry = page.slot(selectedIndex);

        if (!entry.isSelectable(this.openMode)) {
            this.onClose();
            return;
        }

        if (this.openMode == WheelOpenMode.TAP && entry.hasSubmenu()) {
            this.menuStack.addLast(entry.submenu());
            this.currentPageIndex = 0;
            this.rebuildWheelWidget();
            return;
        }

        if (entry.action() != null) {
            entry.action().trigger(new WheelActionContext(
                this.currentPageIndex,
                selectedIndex,
                entry.id(),
                this.openMode
            ));
        }
        this.onClose();
    }

    private void rebuildWheelWidget() {
        WheelPageModel page = this.currentPage();
        List<WheelWidget.RawSection> sections = page.slots().stream()
            .map(entry -> new WheelWidget.RawSection(entry.label(), entry.renderer()::render, entry.isSelectable(this.openMode)))
            .toList();

        this.wheelWidget = new WheelWidget(
            0,
            0,
            this.width,
            this.height,
            Math.min(this.width, this.height) * 0.16f,
            Math.min(this.width, this.height) * 0.28f,
            sections
        );
        this.wheelWidget.clearSelection();
    }

    private int pageCountForCurrentMenu() {
        return this.model.pageCount(this.currentMenuEntries());
    }

    private WheelPageModel currentPage() {
        List<WheelEntry> entries = this.currentMenuEntries();
        int pageCount = this.model.pageCount(entries);
        if (this.currentPageIndex >= pageCount) {
            this.currentPageIndex = 0;
        }
        return this.model.page(entries, this.currentPageIndex);
    }

    private List<WheelEntry> currentMenuEntries() {
        return this.menuStack.getLast();
    }
}

