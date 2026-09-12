package dev.anvilcraft.lib.v2.registrum.client.gui;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
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
    private static final int COLUMNS = 4;
    private static final int TEXTURE_WIDTH = 78;
    private static final int TEXTURE_HEIGHT = 80;
    private static final int CELL_OFFSET = 3;
    private static final int CELL_SIZE = 18;
    private static final int ITEM_OFFSET = CELL_OFFSET + 1;
    private static final int SOURCE_SLOT_SIZE = 18;
    private static final int SOURCE_ITEM_CENTER = 8;
    private static final int VERTICAL_GAP = 0;

    private final Slot sourceSlot;
    private final ItemStack sourceStack;
    private final List<ItemStack> variants;

    private CreativeVariantPickerOverlay(Slot sourceSlot, ItemStack sourceStack, List<ItemStack> variants) {
        this.sourceSlot = sourceSlot;
        this.sourceStack = sourceStack;
        this.variants = variants;
    }

    public static Optional<CreativeVariantPickerOverlay> create(Slot sourceSlot) {
        ItemStack sourceStack = sourceSlot.getItem();
        List<ItemStack> variants = CreativeVariantPickerRegistry.createVariants(sourceStack).orElse(List.of());
        if (variants.isEmpty()) return Optional.empty();
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
        return mouseX >= left && mouseX < left + TEXTURE_WIDTH
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
        graphics.pose().pushMatrix();
        try {
            graphics.nextStratum();
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left,
                top,
                0,
                0,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
            );
            for (int index = 0; index < this.variants.size(); index++) {
                int itemX = left + ITEM_OFFSET + index % COLUMNS * CELL_SIZE;
                int itemY = top + ITEM_OFFSET + index / COLUMNS * CELL_SIZE;
                ItemStack variant = this.variants.get(index);
                graphics.renderItem(variant, itemX, itemY, index);
                graphics.renderItemDecorations(Minecraft.getInstance().font, variant, itemX, itemY);
            }
            int hoveredIndex = this.variantIndexAt(guiLeft, guiTop, mouseX, mouseY);
            if (hoveredIndex >= 0) {
                int itemX = left + ITEM_OFFSET + hoveredIndex % COLUMNS * CELL_SIZE;
                int itemY = top + ITEM_OFFSET + hoveredIndex / COLUMNS * CELL_SIZE;
                graphics.fill(itemX, itemY, itemX + 16, itemY + 16, 0x80FFFFFF);
            }
        } finally {
            graphics.pose().popMatrix();
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
        graphics.pose().pushMatrix();
        graphics.nextStratum();
        try {
            graphics.setTooltipForNextFrame(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
        } finally {
            graphics.pose().popMatrix();
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
        int index = row * COLUMNS + column;
        return index >= this.variants.size() ? -1 : index;
    }

    private void renderCarriedItem(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty()) return;
        int itemX = mouseX - 8;
        int itemY = mouseY - 8;
        graphics.pose().pushMatrix();
        try {
            graphics.nextStratum();
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
            graphics.pose().popMatrix();
        }
    }

    private int left(int guiLeft) {
        int preferred = guiLeft + this.sourceSlot.x + SOURCE_ITEM_CENTER - TEXTURE_WIDTH / 2;
        int maximum = Minecraft.getInstance().getWindow().getGuiScaledWidth() - TEXTURE_WIDTH;
        return Math.max(0, Math.min(preferred, maximum));
    }

    private int top(int guiTop) {
        int sourceSlotTop = guiTop + this.sourceSlot.y - (SOURCE_SLOT_SIZE - 16) / 2;
        int above = sourceSlotTop - VERTICAL_GAP - TEXTURE_HEIGHT;
        if (above >= 0) return above;
        // 小窗口上方不足一整块面板时改放槽位下方，仍不足时限制在可见区域。
        int maximum = Minecraft.getInstance().getWindow().getGuiScaledHeight() - TEXTURE_HEIGHT;
        return Math.max(0, Math.min(sourceSlotTop + SOURCE_SLOT_SIZE + VERTICAL_GAP, maximum));
    }
}
