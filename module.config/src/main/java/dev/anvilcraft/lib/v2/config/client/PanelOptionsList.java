package dev.anvilcraft.lib.v2.config.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/** 让 NeoForge 生成的配置控件在面板内部布局，而不是固定在屏幕中间。 */
final class PanelOptionsList extends OptionsList {
    static final int MENU_ROW_HEIGHT = 25;
    private final List<Row> rows = new ArrayList<>();
    private boolean stacked;
    private final Set<AbstractWidget> immediateSliders = new HashSet<>();

    PanelOptionsList(OptionsSubScreen screen) {
        super(Minecraft.getInstance(), screen.width, screen);
    }

    @Override
    public void clearEntries() {
        super.clearEntries();
        rows.clear();
        immediateSliders.clear();
    }

    @Override
    public void addSmall(AbstractWidget label, @Nullable AbstractWidget control) {
        Row row = new Row(label, control);
        rows.add(row);
        addEntry(row, row.rowHeight());
    }

    void bounds(int x, int y, int width, int height) {
        if (getX() == x && getY() == y && getWidth() == width && getHeight() == height) return;
        double scroll = scrollAmount();
        var focused = getFocused();
        stacked = width < 330;
        updateSizeAndPosition(width, height, x, y);
        super.clearEntries();
        for (Row row : rows) addEntry(row, row.rowHeight());
        setScrollAmount(scroll);
        if (focused != null) setFocused(focused);
    }

    void focusWidget(AbstractWidget widget) {
        for (Row row : rows) {
            if (row.label == widget || row.control == widget) {
                setFocused(row);
                row.setFocused(widget);
                return;
            }
        }
    }

    @Override
    public int getRowWidth() {
        return Math.max(0, getWidth() - (scrollable() ? scrollbarWidth() + 2 : 0));
    }

    @Override
    public int getRowLeft() {
        return getX();
    }

    @Override
    protected int scrollBarX() {
        // 原版按居中的固定行宽放置滚动条，会落在自定义面板的命中范围之外。
        return getRight() - scrollbarWidth();
    }

    int preferredHeight() {
        return contentHeight();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (updateScrolling(event)) {
            int top = scrollBarY();
            int thumb = scrollerHeight();
            if (event.y() < top || event.y() >= top + thumb) {
                double fraction = (event.y() - getY() - thumb / 2.0) / Math.max(1, getHeight() - thumb);
                setScrollAmount(fraction * maxScrollAmount());
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor graphics) {}

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor graphics) {}

    void immediateSlider(AbstractWidget slider) {
        immediateSliders.add(slider);
    }

    @Override
    public void applyUnsavedChanges() {
        for (Row row : rows) {
            // Immediate sliders have already applied every edit. Flushing an untouched stepped
            // slider would otherwise round an existing value that lies between steps.
            if (row.control instanceof OptionInstance.OptionInstanceSliderButton<?> slider
                && !immediateSliders.contains(slider)) slider.applyUnsavedValue();
        }
    }

    private final class Row extends OptionsList.AbstractEntry {
        private final AbstractWidget label;
        private final @Nullable AbstractWidget control;

        private Row(AbstractWidget label, @Nullable AbstractWidget control) {
            this.label = label;
            this.control = control;
        }

        private int rowHeight() {
            return control == null ? MENU_ROW_HEIGHT : stacked ? 50 : 32;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            if (control == null) {
                // 单按钮行只绘制按钮背景，相邻选项之间恰好留一个 GUI 像素。
                label.setRectangle(width, MENU_ROW_HEIGHT - 1, x, y);
            } else if (stacked) {
                graphics.fill(x, y, x + width, y + getHeight() - 4, SidebarConfigScreen.backgroundColor(hovered ? 0xFF252F3D : 0xFF202733, hovered ? 0x70 : 0x50));
                label.setRectangle(width - 16, 14, x + 8, y + 4);
                control.setRectangle(width - 16, 20, x + 8, y + 22);
            } else {
                graphics.fill(x, y, x + width, y + getHeight() - 4, SidebarConfigScreen.backgroundColor(hovered ? 0xFF252F3D : 0xFF202733, hovered ? 0x70 : 0x50));
                int controlWidth = Math.min(200, (width - 24) / 2);
                label.setRectangle(width - controlWidth - 24, 20, x + 8, y + 4);
                control.setRectangle(controlWidth, 20, x + width - controlWidth - 8, y + 4);
            }
            label.extractRenderState(graphics, mouseX, mouseY, partialTick);
            if (control != null) control.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return control == null ? List.of(label) : List.of(label, control);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return control == null ? List.of(label) : List.of(label, control);
        }
    }
}
