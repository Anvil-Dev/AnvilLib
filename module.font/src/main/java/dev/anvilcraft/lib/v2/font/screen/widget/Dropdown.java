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
import java.util.function.Consumer;

public class Dropdown extends AbstractWidget {
    private final List<DropdownEntry> allows = new ArrayList<>();
    private final Minecraft minecraft = Minecraft.getInstance();

    private @Nullable DropdownEntry value = null;
    private boolean expanded = false;
    private int hoveredIndex = -1;
    @Setter
    private Consumer<@Nullable DropdownEntry> onValueChanged = dropdownEntry -> {
    };

    public Dropdown(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void setAllow(List<DropdownEntry> allows) {
        this.allows.clear();
        this.allows.addAll(allows);
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

    public @Nullable DropdownEntry getValue() {
        return this.value;
    }

    public @Nullable String getValueId() {
        return this.value == null ? null : this.value.id;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        this.hoveredIndex = this.getEntryIndexAt(mouseX, mouseY);

        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = x1 + this.width;
        int y2 = y1 + this.height;

        int borderColor = this.isHoveredOrFocused() ? 0xFFE0E0E0 : 0xFF909090;
        int bgColor = this.active ? 0xCC202020 : 0xCC151515;
        guiGraphicsExtractor.fill(x1, y1, x2, y2, borderColor);
        guiGraphicsExtractor.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, bgColor);

        Component valueText = this.value == null ? Component.empty() : this.value.desc;
        guiGraphicsExtractor.centeredText(this.minecraft.font, valueText, x1 + (this.width / 2), y1 + (this.height - 8) / 2, 0xFFFFFFFF);
        guiGraphicsExtractor.centeredText(
            this.minecraft.font,
            Component.literal(this.expanded ? "▲" : "▼"),
            x2 - 8,
            y1 + (this.height - 8) / 2,
            0xFFFFFFFF
        );

        if (!this.expanded) {
            return;
        }

        for (int i = 0; i < this.allows.size(); i++) {
            int rowTop = this.getY() + this.height + i * this.height;
            int rowBottom = rowTop + this.height;
            int rowBg = i == this.hoveredIndex ? 0xCC3F3F3F : 0xCC1F1F1F;

            guiGraphicsExtractor.fill(x1, rowTop, x2, rowBottom, borderColor);
            guiGraphicsExtractor.fill(x1 + 1, rowTop + 1, x2 - 1, rowBottom - 1, rowBg);

            DropdownEntry entry = this.allows.get(i);
            guiGraphicsExtractor.centeredText(
                this.minecraft.font,
                entry.desc,
                x1 + (this.width / 2),
                rowTop + (this.height - 8) / 2,
                0xFFFFFFFF
            );
        }
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
            return true;
        }

        if (!this.expanded) {
            return false;
        }

        int index = this.getEntryIndexAt(mouseX, mouseY);
        if (index < 0) {
            this.expanded = false;
            return false;
        }

        DropdownEntry selected = this.allows.get(index);
        if (!selected.equals(this.value)) {
            this.value = selected;
            this.onValueChanged.accept(this.value);
        }
        this.expanded = false;
        return true;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.visible) {
            return false;
        }
        return this.isPointInMainBox(mouseX, mouseY) || this.isPointInDropdownList(mouseX, mouseY);
    }

    private boolean isPointInMainBox(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + this.width && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }

    private boolean isPointInDropdownList(double mouseX, double mouseY) {
        if (!this.expanded || this.allows.isEmpty()) {
            return false;
        }
        int listTop = this.getY() + this.height;
        int listBottom = listTop + this.allows.size() * this.height;
        return mouseX >= this.getX() && mouseX < this.getX() + this.width && mouseY >= listTop && mouseY < listBottom;
    }

    private int getEntryIndexAt(double mouseX, double mouseY) {
        if (!this.isPointInDropdownList(mouseX, mouseY)) {
            return -1;
        }
        int listTop = this.getY() + this.height;
        int index = (int) ((mouseY - listTop) / this.height);
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

    public record DropdownEntry(Component desc, String id) {
        public static DropdownEntry create(String id) {
            return new DropdownEntry(Component.translatable(Util.makeDescriptionId("dropdown", AnvilLibFont.of(id))), id);
        }

        public static DropdownEntry create(String desc, String id) {
            return new DropdownEntry(Component.literal(desc), id);
        }
    }
}
