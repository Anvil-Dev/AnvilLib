package dev.anvilcraft.lib.v2.registrum.client.gui;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Optional;

/** 锚定创造物品格的通用 4x4 变体选择叠加层。 */
public final class CreativeVariantPickerOverlay {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "anvillib",
        "textures/gui/background/16_color_overlay.png"
    );
    private static final ResourceLocation SCROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "anvillib", "textures/gui/background/groups.png"
    );
    private static final ResourceLocation SLIDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "anvillib", "textures/gui/background/overlay_slider.png"
    );
    private static final int COLUMNS = VariantPickerGrid.COLUMNS;
    private static final int SCROLL_TEXTURE_WIDTH = 85;
    private static final int TRACK_X = 77;
    private static final int TRACK_Y = 4;
    private static final int TRACK_HEIGHT = 70;
    private static final int SLIDER_WIDTH = 4;
    private static final int SLIDER_HEIGHT = 12;
    private static final int TEXTURE_WIDTH = 78;
    private static final int TEXTURE_HEIGHT = 80;
    private static final int CELL_OFFSET = 3;
    private static final int CELL_SIZE = 18;
    private static final int ITEM_OFFSET = CELL_OFFSET + 1;
    private static final int SOURCE_SLOT_SIZE = 18;
    private static final int SOURCE_ITEM_CENTER = 8;
    private static final int VERTICAL_GAP = 0;
    private static final int BACKGROUND_Z = 500;
    private static final int HIGHLIGHT_Z = 200;
    private static final int CARRIED_ITEM_Z = 600;

    private final Slot sourceSlot;
    private final ItemStack sourceStack;
    private final List<ItemStack> variants;
    private final VariantPickerGrid grid;
    private boolean draggingSlider;
    private double sliderGrabOffset;

    private CreativeVariantPickerOverlay(Slot sourceSlot, ItemStack sourceStack, List<ItemStack> variants) {
        this.sourceSlot = sourceSlot;
        this.sourceStack = sourceStack;
        this.variants = variants;
        this.grid = new VariantPickerGrid(variants.size());
    }

    public static Optional<CreativeVariantPickerOverlay> create(Slot sourceSlot) {
        ItemStack sourceStack = sourceSlot.getItem();
        List<ItemStack> variants = CreativeVariantPickerRegistry.createVariants(sourceStack).orElse(List.of());
        if (variants.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CreativeVariantPickerOverlay(sourceSlot, sourceStack.copyWithCount(1), variants));
    }

    public boolean isValid() {
        return this.sourceSlot.isActive()
            && ItemStack.isSameItemSameComponents(this.sourceSlot.getItem(), this.sourceStack)
            && CreativeVariantPickerRegistry.isCreativePickerEnabled(this.sourceSlot.getItem());
    }

    public Slot sourceSlot() {
        return this.sourceSlot;
    }

    public boolean contains(int guiLeft, int guiTop, double mouseX, double mouseY) {
        int left = this.left(guiLeft);
        int top = this.top(guiTop);
        return mouseX >= left && mouseX < left + this.width()
            && mouseY >= top && mouseY < top + TEXTURE_HEIGHT;
    }

    public Optional<ItemStack> variantAt(int guiLeft, int guiTop, double mouseX, double mouseY) {
        int index = this.variantIndexAt(guiLeft, guiTop, mouseX, mouseY);
        return index < 0 ? Optional.empty() : Optional.of(this.variants.get(index));
    }

    public void render(
        GuiGraphics graphics,
        int guiLeft,
        int guiTop,
        int mouseX,
        int mouseY,
        ItemStack carriedStack
    ) {
        int left = this.left(guiLeft);
        int top = this.top(guiTop);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0.0F, 0.0F, BACKGROUND_Z);
            graphics.blit(
                net.minecraft.client.renderer.RenderType::guiTextured,
                this.grid.isScrollable() ? SCROLL_TEXTURE : TEXTURE,
                left,
                top,
                0,
                0,
                this.width(),
                TEXTURE_HEIGHT,
                this.width(),
                TEXTURE_HEIGHT
            );
            this.renderSlider(graphics, left, top, mouseX, mouseY);
            for (int cell = 0; cell < this.grid.visibleCount(); cell++) {
                int index = this.grid.indexAt(cell);
                int itemX = left + ITEM_OFFSET + cell % COLUMNS * CELL_SIZE;
                int itemY = top + ITEM_OFFSET + cell / COLUMNS * CELL_SIZE;
                ItemStack variant = this.variants.get(index);
                graphics.renderItem(variant, itemX, itemY, index);
                graphics.renderItemDecorations(Minecraft.getInstance().font, variant, itemX, itemY);
            }
            int hoveredIndex = this.variantIndexAt(guiLeft, guiTop, mouseX, mouseY);
            if (hoveredIndex >= 0) {
                int cell = hoveredIndex - this.grid.firstIndex();
                int itemX = left + ITEM_OFFSET + cell % COLUMNS * CELL_SIZE;
                int itemY = top + ITEM_OFFSET + cell / COLUMNS * CELL_SIZE;
                graphics.fill(itemX, itemY, itemX + 16, itemY + 16, HIGHLIGHT_Z, 0x80FFFFFF);
            }
        } finally {
            graphics.pose().popPose();
        }
        this.renderCarriedItem(graphics, carriedStack, mouseX, mouseY);
    }

    public void renderTooltip(
        GuiGraphics graphics,
        Font font,
        List<Component> lines,
        ItemStack stack,
        int mouseX,
        int mouseY
    ) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, BACKGROUND_Z);
        try {
            graphics.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
        } finally {
            graphics.pose().popPose();
        }
    }

    private int variantIndexAt(int guiLeft, int guiTop, double mouseX, double mouseY) {
        int localX = (int) Math.floor(mouseX - this.left(guiLeft));
        int localY = (int) Math.floor(mouseY - this.top(guiTop));
        int gridSize = COLUMNS * CELL_SIZE;
        if (localX < CELL_OFFSET || localX >= CELL_OFFSET + gridSize
            || localY < CELL_OFFSET || localY >= CELL_OFFSET + gridSize) {
            return -1;
        }
        int column = (localX - CELL_OFFSET) / CELL_SIZE;
        int row = (localY - CELL_OFFSET) / CELL_SIZE;
        return this.grid.indexAt(row * COLUMNS + column);
    }

    private void renderCarriedItem(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty()) return;
        int itemX = mouseX - 8;
        int itemY = mouseY - 8;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0.0F, 0.0F, CARRIED_ITEM_Z);
            graphics.renderItem(stack, itemX, itemY);
            Font itemFont = IClientItemExtensions.of(stack).getFont(
                stack,
                IClientItemExtensions.FontContext.ITEM_COUNT
            );
            graphics.renderItemDecorations(
                itemFont == null ? Minecraft.getInstance().font : itemFont,
                stack,
                itemX,
                itemY
            );
        } finally {
            graphics.pose().popPose();
        }
    }

    /** 按行滚动变体，保留触控板产生的不足一行的滚动量。 */
    public void scroll(double amount) {
        this.grid.scroll(amount);
    }

    /** 点击轨道时移动滑条，点击滑条时保留鼠标的抓取位置。 */
    public boolean beginSliderDrag(int guiLeft, int guiTop, double mouseX, double mouseY) {
        int trackLeft = this.left(guiLeft) + TRACK_X;
        int trackTop = this.top(guiTop) + TRACK_Y;
        if (!this.grid.isScrollable() || mouseX < trackLeft || mouseX >= trackLeft + SLIDER_WIDTH
            || mouseY < trackTop || mouseY >= trackTop + TRACK_HEIGHT) {
            return false;
        }
        int sliderTop = this.sliderTop(this.top(guiTop));
        this.sliderGrabOffset = mouseY >= sliderTop && mouseY < sliderTop + SLIDER_HEIGHT
            ? mouseY - sliderTop : SLIDER_HEIGHT / 2d;
        this.draggingSlider = true;
        this.dragSlider(guiTop, mouseY);
        return true;
    }

    /** 拖动期间允许指针离开轨道，并将位置限制在首尾行。 */
    public void dragSlider(int guiTop, double mouseY) {
        if (this.draggingSlider) {
            this.grid.scrollTo((mouseY - this.top(guiTop) - TRACK_Y - this.sliderGrabOffset)
                / (TRACK_HEIGHT - SLIDER_HEIGHT));
        }
    }

    /** 松开鼠标后结束滑条拖动。 */
    public void endSliderDrag() {
        this.draggingSlider = false;
    }

    private void renderSlider(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        if (!this.grid.isScrollable()) {
            return;
        }
        int sliderLeft = left + TRACK_X;
        int sliderTop = this.sliderTop(top);
        boolean hovered = mouseX >= sliderLeft && mouseX < sliderLeft + SLIDER_WIDTH
            && mouseY >= sliderTop && mouseY < sliderTop + SLIDER_HEIGHT;
        graphics.blit(
            net.minecraft.client.renderer.RenderType::guiTextured,
            SLIDER_TEXTURE,
            sliderLeft,
            sliderTop,
            hovered || this.draggingSlider ? SLIDER_WIDTH : 0,
            0,
            SLIDER_WIDTH,
            SLIDER_HEIGHT,
            SLIDER_WIDTH * 2,
            SLIDER_HEIGHT
        );
    }

    private int sliderTop(int top) {
        return top + TRACK_Y + (int) Math.round(this.grid.scrollFraction() * (TRACK_HEIGHT - SLIDER_HEIGHT));
    }

    private int width() {
        return this.grid.isScrollable() ? SCROLL_TEXTURE_WIDTH : TEXTURE_WIDTH;
    }

    private int left(int guiLeft) {
        return guiLeft + this.sourceSlot.x + SOURCE_ITEM_CENTER - this.width() / 2;
    }

    private int top(int guiTop) {
        int sourceSlotTop = guiTop + this.sourceSlot.y - (SOURCE_SLOT_SIZE - 16) / 2;
        int above = sourceSlotTop - VERTICAL_GAP - TEXTURE_HEIGHT;
        if (above >= 0) return above;
        // 顶部不足时放到源槽位下方，并约束在当前窗口内。
        int maximum = Minecraft.getInstance().getWindow().getGuiScaledHeight() - TEXTURE_HEIGHT;
        return Math.max(0, Math.min(sourceSlotTop + SOURCE_SLOT_SIZE + VERTICAL_GAP, maximum));
    }
}
