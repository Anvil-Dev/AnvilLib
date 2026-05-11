package dev.anvilcraft.lib.v2.font.screen.widget;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Dropdown extends AbstractWidget {
    private final List<DropdownEntry> allows = new ArrayList<>();
    private final Minecraft minecraft = Minecraft.getInstance();
    private @Nullable DropdownEntry value = null;
    private boolean expanded = false;
    @Setter
    private Consumer<@Nullable DropdownEntry> onValueChanged = _ -> {
    };
    @Setter
    private Consumer<Shielding> onShieldingAdd = _ -> {
    };
    @Setter
    private Runnable onShieldingRemove = () -> {
    };
    @Setter
    private Supplier<@Nullable Shielding> shieldingGetter = () -> null;
    private final int screenWidth;
    private final int screenHeight;
    private int scrollOffset = 0;
    private boolean draggingScrollbar;

    public Dropdown(int x, int y, int width, int height, int screenWidth, int screenHeight, Component message) {
        super(x, y, width, height, message);
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void setAllow(List<DropdownEntry> allows) {
        this.allows.clear();
        this.allows.addAll(allows);
        this.scrollOffset = 0;
        if (this.allows.contains(this.value)) {
            return;
        }
        this.value = this.allows.isEmpty() ? null : this.allows.getFirst();
    }

    public void setValue(@Nullable DropdownEntry value) {
        if (value != null && !this.allows.contains(value)) {
            return;
        }
        this.value = value;
    }


    public void setValue(@Nullable String value) {
        for (DropdownEntry allow : this.allows) {
            if (Objects.equals(allow.id, value)) {
                this.setValue(allow);
                return;
            }
        }
    }

    public @Nullable DropdownEntry getValue() {
        return this.value;
    }

    public @Nullable String getValueId() {
        return this.value == null ? null : this.value.id;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        int hoveredIndex = this.getEntryIndexAt(mouseX, mouseY);

        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = x1 + this.getWidth();
        int y2 = y1 + this.getHeight();

        int borderColor = this.isHoveredOrFocused() ? 0xFFE0E0E0 : 0xFF909090;
        int bgColor = this.active ? 0xCC202020 : 0xCC151515;
        guiGraphicsExtractor.fill(x1, y1, x2, y2, borderColor);
        guiGraphicsExtractor.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, bgColor);

        Component valueText = this.value == null ? Component.empty() : this.value.desc;
        guiGraphicsExtractor.centeredText(
            this.minecraft.font,
            valueText,
            x1 + (this.getWidth() / 2),
            y1 + (this.getHeight() - 8) / 2,
            0xFFFFFFFF
        );
        guiGraphicsExtractor.centeredText(
            this.minecraft.font,
            Component.literal(this.expanded ? "▲" : "▼"),
            x2 - 8,
            y1 + (this.getHeight() - 8) / 2,
            0xFFFFFFFF
        );

        if (!this.expanded) {
            return;
        }

        int startHeight = this.getY() + this.getHeight();
        int listAreaHeight = this.calcMaxHeight();
        int maxY = startHeight + listAreaHeight;
        boolean needsScrollbar = this.maxScrollOffset() > 0;
        int scrollbarWidth = needsScrollbar ? 5 : 0;

        guiGraphicsExtractor.enableScissor(x1, startHeight, Math.min(this.screenWidth, x2), maxY);
        for (int i = 0; i < this.allows.size(); i++) {
            int rowTop = startHeight + (i - this.scrollOffset) * this.getHeight();
            int rowBottom = rowTop + this.getHeight();

            if (rowBottom <= startHeight || rowTop >= maxY) continue;

            int rowBg = i == hoveredIndex ? 0xCC3F3F3F : 0xCC1F1F1F;
            guiGraphicsExtractor.fill(x1, rowTop, x2, rowBottom, borderColor);
            guiGraphicsExtractor.fill(x1 + 1, rowTop + 1, x2 - 1, rowBottom - 1, rowBg);

            DropdownEntry entry = this.allows.get(i);
            guiGraphicsExtractor.centeredText(
                this.minecraft.font,
                entry.desc,
                x1 + (this.getWidth() - scrollbarWidth) / 2,
                rowTop + (this.getHeight() - 8) / 2,
                0xFFFFFFFF
            );
        }
        guiGraphicsExtractor.disableScissor();

        if (needsScrollbar) {
            int visibleRows = this.visibleRowCount();
            int totalRows = this.allows.size();
            int thumbHeight = Math.max(10, listAreaHeight * visibleRows / totalRows);
            int maxScroll = this.maxScrollOffset();
            int thumbTop = maxScroll == 0 ? 0 : (listAreaHeight - thumbHeight) * this.scrollOffset / maxScroll;

            // track
            guiGraphicsExtractor.fill(x2 - scrollbarWidth, startHeight, x2, maxY, 0xFF303030);
            // thumb
            guiGraphicsExtractor.fill(
                x2 - scrollbarWidth + 1,
                startHeight + thumbTop,
                x2 - 1,
                startHeight + thumbTop + thumbHeight,
                0xFF909090
            );
        }
    }

    public int calcMaxHeight() {
        int startHeight = this.getY() + this.getHeight();
        return Math.clamp((long) this.allows.size() * this.getHeight(), 0, this.screenHeight - startHeight - 10);
    }

    private int visibleRowCount() {
        return this.calcMaxHeight() / this.getHeight();
    }

    private int maxScrollOffset() {
        return Math.max(0, this.allows.size() - this.visibleRowCount());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.expanded || scrollY == 0 || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scrollOffset = Math.clamp(this.scrollOffset - (int) Math.signum(scrollY), 0, this.maxScrollOffset());
        return true;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.handlePrimaryClick(event.x(), event.y(), event.button());
    }

    private boolean handlePrimaryClick(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0) {
            return false;
        }

        if (this.isPointInMainBox(mouseX, mouseY)) {
            this.expanded = !this.expanded;
            this.shielding();
            return true;
        }

        if (!this.expanded) {
            return false;
        }

        // Click on scrollbar: start dragging
        if (this.needsScrollbar() && this.isOnScrollbar(mouseX, mouseY)) {
            this.draggingScrollbar = true;
            this.scrollToMouse(mouseY);
            return true;
        }

        int index = this.getEntryIndexAt(mouseX, mouseY);
        if (index < 0) {
            this.expanded = false;
            this.removeShielding();
            return false;
        }

        DropdownEntry selected = this.allows.get(index);
        if (!selected.equals(this.value)) {
            this.value = selected;
            this.onValueChanged.accept(this.value);
        }
        this.expanded = false;
        this.removeShielding();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.draggingScrollbar) {
            this.scrollToMouse(event.y());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingScrollbar = false;
        return false;
    }

    private boolean needsScrollbar() {
        return this.maxScrollOffset() > 0;
    }

    private boolean isOnScrollbar(double mouseX, double mouseY) {
        int x2 = this.getX() + this.getWidth();
        int listTop = this.getY() + this.getHeight();
        int listBottom = listTop + this.calcMaxHeight();
        return mouseX >= x2 - 5 && mouseX < x2 && mouseY >= listTop && mouseY < listBottom;
    }

    private void scrollToMouse(double mouseY) {
        int listTop = this.getY() + this.getHeight();
        int listHeight = this.calcMaxHeight();
        int thumbHeight = Math.max(10, listHeight * this.visibleRowCount() / this.allows.size());
        int trackHeight = listHeight - thumbHeight;
        int maxScroll = this.maxScrollOffset();
        if (trackHeight <= 0 || maxScroll <= 0) return;
        int relativeY = (int) Math.clamp(mouseY - listTop - thumbHeight / 2.0, 0, trackHeight);
        this.scrollOffset = relativeY * maxScroll / trackHeight;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        Shielding shielding = this.shieldingGetter.get();
        if (!this.visible || (shielding != null && shielding.isShielding(mouseX, mouseY, this))) {
            return false;
        }
        return this.isPointInMainBox(mouseX, mouseY) || this.isPointInDropdownList(mouseX, mouseY);
    }

    private boolean isPointInMainBox(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight();
    }

    private boolean isPointInDropdownList(double mouseX, double mouseY) {
        if (!this.expanded || this.allows.isEmpty()) {
            return false;
        }
        int listTop = this.getY() + this.getHeight();
        int listBottom = listTop + this.calcMaxHeight();
        return mouseX >= this.getX() && mouseX < this.getX() + this.getWidth() && mouseY >= listTop && mouseY < listBottom;
    }

    private int getEntryIndexAt(double mouseX, double mouseY) {
        if (!this.isPointInDropdownList(mouseX, mouseY)) {
            return -1;
        }
        int listTop = this.getY() + this.getHeight();
        int index = (int) ((mouseY - listTop) / this.getHeight()) + this.scrollOffset;
        return index >= 0 && index < this.allows.size() ? index : -1;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
        narrationElementOutput.add(NarratedElementType.USAGE, this.value == null ? Component.empty() : this.value.desc);
        if (this.expanded) {
            narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.anvillib_font.dropdown.expanded"));
        } else {
            narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.anvillib_font.dropdown.collapsed"));
        }
    }

    protected void shielding() {
        if (this.expanded) {
            this.createShielding(
                this.getX(),
                this.getY() + this.getHeight(),
                this.getX() + this.getWidth(),
                this.getY() + this.getHeight() + this.calcMaxHeight()
            );
            return;
        }
        this.removeShielding();
    }

    protected void createShielding(int x1, int y1, int x2, int y2) {
        this.onShieldingAdd.accept(new Shielding(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), this));
    }

    protected void removeShielding() {
        this.onShieldingRemove.run();
    }

    public record DropdownEntry(Component desc, String id) {
        public static DropdownEntry create(String id) {
            return new DropdownEntry(Component.translatable(Util.makeDescriptionId("dropdown", AnvilLibFont.of(id))), id);
        }

        public static DropdownEntry create(String desc, String id) {
            return new DropdownEntry(Component.literal(desc), id);
        }
    }

    public record Shielding(double x1, double y1, double x2, double y2, Dropdown dropdown) {
        public boolean isShielding(double x, double y, Dropdown dropdown) {
            if (Objects.equals(this.dropdown(), dropdown)) return false;
            return x >= this.x1() && x < this.x2() && y >= this.y1() && y < this.y2();
        }
    }
}
