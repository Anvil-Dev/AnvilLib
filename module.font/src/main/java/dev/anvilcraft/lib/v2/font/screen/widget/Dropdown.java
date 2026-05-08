package dev.anvilcraft.lib.v2.font.screen.widget;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Dropdown extends AbstractWidget {
    private final List<DropdownEntry> allows = new ArrayList<>();
    private @Nullable DropdownEntry value = null;

    public Dropdown(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void setAllow(List<DropdownEntry> allows) {
        this.allows.clear();
        this.allows.addAll(allows);
        if (this.allows.contains(this.value)) return;
        this.value = allows.isEmpty() ? null : allows.getFirst();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
        narrationElementOutput.add(NarratedElementType.USAGE, this.value == null ? Component.empty() : this.value.desc);
    }

    public record DropdownEntry(Component desc, String id) {
        public static DropdownEntry create(String id) {
            return new DropdownEntry(Component.translatable(Util.makeDescriptionId("dropdown", AnvilLibFont.of(id))), id);
        }
    }
}
