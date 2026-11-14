package dev.anvilcraft.lib.v2.rendering.preview.level;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 按实际字体宽度换行并翻页，保证较长的方块实体配置不会超出窗口。 */
public final class StructureDetailsPager {
    private List<Component> source = List.of();
    private List<Component> wrapped = List.of();
    private int width;
    private int rows = 10;
    private int page;

    public void setLines(List<Component> lines) {
        this.source = List.copyOf(lines);
        this.wrapped = List.of();
        this.width = 0;
        this.page = 0;
    }

    public List<Component> visible(Font font, int screenWidth, int screenHeight) {
        int nextWidth = Math.max(1, Math.min(320, screenWidth - 24));
        int nextRows = Math.max(1, Math.min(12, (screenHeight - 24) / (font.lineHeight + 1) - 2));
        if (this.width != nextWidth || this.rows != nextRows) {
            this.width = nextWidth;
            this.rows = nextRows;
            this.page = 0;
            List<Component> result = new ArrayList<>();
            for (Component line : this.source) {
                for (var sequence : font.getSplitter().splitLines(line, this.width, Style.EMPTY)) {
                    MutableComponent text = Component.empty();
                    sequence.visit((style, value) -> {
                        text.append(Component.literal(value).withStyle(style));
                        return Optional.empty();
                    }, Style.EMPTY);
                    result.add(text);
                }
            }
            this.wrapped = List.copyOf(result);
        }
        return page(this.wrapped, this.page, this.rows);
    }

    static List<Component> page(List<Component> lines, int page, int rows) {
        if (lines.isEmpty()) {
            return List.of();
        }
        rows = Math.max(1, rows);
        int pages = (lines.size() + rows - 1) / rows;
        page = Math.max(0, Math.min(pages - 1, page));
        int start = page * rows;
        List<Component> result = new ArrayList<>(lines.subList(start, Math.min(start + rows, lines.size())));
        if (pages > 1) {
            result.add(Component.translatable("tooltip.anvillib_rendering.structure_preview.page", page + 1, pages)
                .withStyle(ChatFormatting.YELLOW));
        }
        return result;
    }

    public void scroll(double amount) {
        this.page = Math.max(0, Math.min(this.pages() - 1, this.page + (amount < 0 ? 1 : -1)));
    }

    private int pages() {
        return Math.max(1, (this.wrapped.size() + this.rows - 1) / this.rows);
    }
}
