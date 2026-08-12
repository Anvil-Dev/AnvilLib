package dev.anvilcraft.lib.v2.wheel.client.gui.screen;

import dev.anvilcraft.lib.v2.wheel.api.WheelActionContext;
import dev.anvilcraft.lib.v2.wheel.api.WheelEntry;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.api.WheelOpenMode;
import dev.anvilcraft.lib.v2.wheel.api.WheelPageModel;
import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelFrostedBackground;
import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;

public class WheelScreen extends Screen {
    private static final float WHEEL_INNER_RADIUS_SCALE = 0.17f;
    private static final float WHEEL_OUTER_RADIUS_SCALE = 0.33f;

    private final WheelMenuModel model;
    private final WheelOpenMode openMode;
    private final Deque<List<WheelEntry>> menuStack = new ArrayDeque<>();

    private WheelWidget wheelWidget;
    private int currentPageIndex;
    @Nullable
    private WheelFrostedBackground frostedBackground;

    public WheelScreen(WheelMenuModel model, WheelOpenMode openMode) {
        super(Component.empty());
        this.model = model;
        this.openMode = openMode;
        this.menuStack.addLast(this.model.rootEntries());
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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.wheelWidget != null) {
            this.wheelWidget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            if (this.menuStack.size() > 1) {
                this.menuStack.removeLast();
                this.currentPageIndex = 0;
                this.rebuildWheelWidget();
            } else {
                this.onClose();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.wheelWidget != null) {
            this.wheelWidget.onClosing();
            return;
        }
        super.onClose();
    }

    @Override
    protected void init() {
        if (this.frostedBackground != null) {
            this.frostedBackground.close();
        }
        this.frostedBackground = new WheelFrostedBackground();
        this.rebuildWheelWidget();
    }

    @Override
    public void removed() {
        if (this.frostedBackground != null) {
            this.frostedBackground.close();
            this.frostedBackground = null;
        }
        super.removed();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (this.openMode == WheelOpenMode.TAP && event.button() == 0) {
            this.triggerSelectedOrClose();
            return true;
        }
        return super.mouseClicked(event, isDoubleClick);
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
            .map(entry -> {
                var renderer = entry.renderer();
                return new WheelWidget.RawSection(
                    entry.label(),
                    renderer == null ? null : renderer::render,
                    entry.isSelectable(this.openMode)
                );
            })
            .toList();

        this.wheelWidget = new WheelWidget(
            0,
            0,
            this.width,
            this.height,
            Math.min(this.width, this.height) * WHEEL_INNER_RADIUS_SCALE,
            Math.min(this.width, this.height) * WHEEL_OUTER_RADIUS_SCALE,
            sections,
            this.model.deadZone()
        );
        this.wheelWidget.setFrostedBackground(this.frostedBackground);
        this.wheelWidget.setPageState(
            this.currentPageIndex > 0,
            this.currentPageIndex < this.pageCountForCurrentMenu() - 1
        );
        this.wheelWidget.setSelectionEffectColor(this.model.selectionEffectColor());
        this.wheelWidget.setSelectionEffect(this.model.selectionEffect());
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

