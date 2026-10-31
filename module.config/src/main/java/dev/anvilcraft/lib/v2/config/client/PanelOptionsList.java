package dev.anvilcraft.lib.v2.config.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 保留原版条目的焦点导航，为旧版固定行高的列表补充面板布局和命中范围。 */
final class PanelOptionsList extends OptionsList {
    static final int MENU_ROW_HEIGHT = 25;
    private static final int SCROLLBAR_WIDTH = 6;
    private final List<Row> rows = new ArrayList<>();
    private boolean stacked;
    private @Nullable Entry hovered;

    PanelOptionsList(OptionsSubScreen screen) {
        super(Minecraft.getInstance(), screen.width, screen);
    }

    @Override
    public void clearEntries() {
        super.clearEntries();
        rows.clear();
    }

    @Override
    public void addSmall(AbstractWidget label, @Nullable AbstractWidget control) {
        rows.add(new Row(label, control));
        super.addSmall(label, control);
    }

    void bounds(int x, int y, int width, int height) {
        if (getX() == x && getY() == y && getWidth() == width && getHeight() == height) return;
        stacked = width < 330;
        setSize(width, height);
        setPosition(x, y);
        clampScrollAmount();
    }

    void focusWidget(AbstractWidget widget) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row.label == widget || row.control == widget) {
                Entry entry = getEntry(i);
                setFocused(entry);
                entry.setFocused(widget);
                return;
            }
        }
    }

    @Override
    public int getRowWidth() {
        return Math.max(0, getWidth() - (scrollbarVisible() ? SCROLLBAR_WIDTH + 2 : 0));
    }

    @Override
    public int getRowLeft() {
        return getX();
    }

    @Override
    public int getRowTop(int index) {
        int top = getY() - (int) getScrollAmount();
        for (int i = 0; i < index; i++) top += rows.get(i).height();
        return top;
    }

    @Override
    public int getRowBottom(int index) {
        return getRowTop(index) + rows.get(index).height();
    }

    @Override
    protected int getMaxPosition() {
        return rows == null ? 0 : rows.stream().mapToInt(Row::height).sum();
    }

    @Override
    protected int getScrollbarPosition() {
        return getRight() - SCROLLBAR_WIDTH;
    }

    int preferredHeight() {
        return getMaxPosition();
    }

    @Override
    public int getMaxScroll() {
        return Math.max(0, getMaxPosition() - getHeight());
    }

    private int scrollerHeight() {
        return Mth.clamp(getHeight() * getHeight() / Math.max(1, getMaxPosition()), 32, getHeight() - 8);
    }

    private int scrollBarY() {
        return getY() + (int) getScrollAmount() * (getHeight() - scrollerHeight()) / Math.max(1, getMaxScroll());
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY) || mouseX >= getRowLeft() + getRowWidth()) return Optional.empty();
        for (int i = 0; i < rows.size(); i++) {
            if (mouseY >= getRowTop(i) && mouseY < getRowBottom(i)) return Optional.of(getEntry(i));
        }
        return Optional.empty();
    }

    @Override
    protected @Nullable Entry getHovered() {
        return hovered;
    }

    @Override
    protected void ensureVisible(Entry entry) {
        int index = children().indexOf(entry);
        if (index < 0) return;
        if (getRowTop(index) < getY()) setScrollAmount(getScrollAmount() + getRowTop(index) - getY());
        if (getRowBottom(index) > getBottom()) setScrollAmount(getScrollAmount() + getRowBottom(index) - getBottom());
    }

    @Override
    protected void centerScrollOn(Entry entry) {
        int index = children().indexOf(entry);
        if (index >= 0) setScrollAmount(getScrollAmount() + getRowTop(index) - getY() + rows.get(index).height() / 2.0 - getHeight() / 2.0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isValidMouseClick(button) || !isMouseOver(mouseX, mouseY)) return false;
        updateScrollingState(mouseX, mouseY, button);
        if (scrollbarVisible() && mouseX >= getScrollbarPosition() && mouseX < getRight()) {
            setFocused(null);
            int top = scrollBarY();
            int thumb = scrollerHeight();
            if (mouseY < top || mouseY >= top + thumb) {
                double fraction = (mouseY - getY() - thumb / 2.0) / Math.max(1, getHeight() - thumb);
                setScrollAmount(fraction * getMaxScroll());
            }
            return true;
        }
        Entry entry = (Entry) getChildAt(mouseX, mouseY).orElse(null);
        if (entry == null || !entry.mouseClicked(mouseX, mouseY, button)) return false;
        if (getFocused() != null && getFocused() != entry) getFocused().setFocused(null);
        setFocused(entry);
        setDragging(true);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        updateScrollingState(mouseX, mouseY, -1);
        setDragging(false);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderListBackground(GuiGraphics graphics) {}

    @Override
    protected void renderListSeparators(GuiGraphics graphics) {}

    @Override
    protected void renderListItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hovered = (Entry) getChildAt(mouseX, mouseY).orElse(null);
        for (int i = 0; i < rows.size(); i++) {
            if (getRowBottom(i) >= getY() && getRowTop(i) <= getBottom()) {
                rows.get(i).render(graphics, getRowTop(i), getEntry(i) == hovered, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public void applyUnsavedChanges() {
        for (Row row : rows) {
            if (row.control instanceof OptionInstance.OptionInstanceSliderButton<?> slider) slider.applyUnsavedValue();
        }
    }

    private final class Row {
        private final AbstractWidget label;
        private final @Nullable AbstractWidget control;

        private Row(AbstractWidget label, @Nullable AbstractWidget control) {
            this.label = label;
            this.control = control;
        }

        private int height() {
            return control == null ? MENU_ROW_HEIGHT : stacked ? 50 : 32;
        }

        private void render(GuiGraphics graphics, int y, boolean hovered, int mouseX, int mouseY, float partialTick) {
            int x = getRowLeft();
            int width = getRowWidth();
            if (control == null) {
                label.setRectangle(width, MENU_ROW_HEIGHT - 1, x, y);
            } else if (stacked) {
                graphics.fill(x, y, x + width, y + height() - 4, hovered ? 0xFF252F3D : 0xFF202733);
                label.setRectangle(width - 16, 14, x + 8, y + 4);
                control.setRectangle(width - 16, 20, x + 8, y + 22);
            } else {
                graphics.fill(x, y, x + width, y + height() - 4, hovered ? 0xFF252F3D : 0xFF202733);
                int controlWidth = Math.min(200, (width - 24) / 2);
                label.setRectangle(width - controlWidth - 24, 20, x + 8, y + 4);
                control.setRectangle(controlWidth, 20, x + width - controlWidth - 8, y + 4);
            }
            label.render(graphics, mouseX, mouseY, partialTick);
            if (control != null) control.render(graphics, mouseX, mouseY, partialTick);
        }
    }
}
