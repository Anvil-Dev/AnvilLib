package dev.anvilcraft.lib.v2.registrum.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.lib.v2.registrum.client.gui.CreativeVariantPickerOverlay;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSection;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** 创造栏分区绘制和变体叠加层的客户端交互。 */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin
    extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Unique
    private static final int anvillib$COLUMN_COUNT = 9;
    @Unique
    private static final int anvillib$VISIBLE_ROW_COUNT = 5;
    @Unique
    private static final int anvillib$CELL_SIZE = CreativeTabSection.BANNER_CELL_SIZE;
    @Unique
    private static final int anvillib$GRID_LEFT = 9;
    @Unique
    private static final int anvillib$GRID_TOP = 18;
    @Unique
    private static final int anvillib$BANNER_Z = 200;
    @Unique
    private static final int anvillib$TEXT_PADDING = CreativeTabSection.DEFAULT_TEXT_PADDING;

    @Shadow
    private static CreativeModeTab selectedTab;
    @Shadow
    @Final
    private static SimpleContainer CONTAINER;
    @Shadow
    private float scrollOffs;

    @Unique
    private CreativeTabSection anvillib$hoveredSection;
    @Unique
    @Nullable
    private CreativeVariantPickerOverlay anvillib$variantOverlay;
    @Unique
    @Nullable
    private ItemStack anvillib$hoveredPickerVariant;
    @Unique
    private int anvillib$consumedMouseButtons;

    protected CreativeModeInventoryScreenMixin(
        CreativeModeInventoryScreen.ItemPickerMenu menu,
        Inventory inventory,
        Component title
    ) {
        super(menu, inventory, title);
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;"
                + "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V"
        )
    )
    private void anvillib$renderSections(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick,
        CallbackInfo ci
    ) {
        this.anvillib$hoveredSection = null;
        if (selectedTab == null || selectedTab.getType() != CreativeModeTab.Type.CATEGORY) return;
        ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(selectedTab);
        if (tabId == null) return;
        int scrollRow = this.anvillib$scrollRow();
        for (CreativeTabSections.PlacedSection placedSection : CreativeTabSections.placedSections(tabId)) {
            this.anvillib$renderSection(graphics, placedSection, scrollRow, mouseX, mouseY);
        }
        if (this.anvillib$hoveredSection != null) this.hoveredSlot = null;
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;"
                + "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V"
        )
    )
    private void anvillib$renderVariantOverlay(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick,
        CallbackInfo ci
    ) {
        CreativeVariantPickerOverlay overlay = this.anvillib$validVariantOverlay();
        if (overlay == null) {
            this.anvillib$hoveredPickerVariant = null;
            return;
        }
        this.anvillib$hoveredPickerVariant = overlay.variantAt(
            this.leftPos,
            this.topPos,
            mouseX,
            mouseY
        ).orElse(null);
        overlay.render(
            graphics,
            this.leftPos,
            this.topPos,
            mouseX,
            mouseY,
            this.menu.getCarried()
        );
        if (overlay.contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            this.anvillib$hoveredSection = null;
            this.hoveredSlot = null;
        }
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;"
                + "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            shift = At.Shift.AFTER
        )
    )
    private void anvillib$renderTooltips(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick,
        CallbackInfo ci
    ) {
        if (this.anvillib$hoveredSection != null) {
            List<Component> tooltip = this.anvillib$hoveredSection.tooltip();
            if (!tooltip.isEmpty()) graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
        CreativeVariantPickerOverlay overlay = this.anvillib$validVariantOverlay();
        ItemStack variant = this.anvillib$hoveredPickerVariant;
        if (overlay == null || variant == null) return;
        Slot previousHoveredSlot = this.hoveredSlot;
        this.hoveredSlot = overlay.sourceSlot();
        try {
            overlay.renderTooltip(
                graphics,
                this.font,
                this.getTooltipFromContainerItem(variant),
                variant,
                mouseX,
                mouseY
            );
        } finally {
            this.hoveredSlot = previousHoveredSlot;
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void anvillib$handleVariantOverlayClick(
        double mouseX,
        double mouseY,
        int button,
        CallbackInfoReturnable<Boolean> cir
    ) {
        CreativeVariantPickerOverlay overlay = this.anvillib$validVariantOverlay();
        if (overlay != null && overlay.contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            this.anvillib$consumeMouseButton(button);
            overlay.variantAt(this.leftPos, this.topPos, mouseX, mouseY).ifPresent(variant -> {
                if (button == 0 || button == 1) {
                    this.anvillib$selectPickerVariant(overlay, variant, button);
                } else if (this.anvillib$isPickerCloneMouseButton(button)) {
                    this.anvillib$clickPickerVariant(overlay, variant, button, ClickType.CLONE);
                } else {
                    this.anvillib$clickPickerHotbarMouseButton(overlay, variant, button);
                }
            });
            cir.setReturnValue(true);
            return;
        }

        Slot sourceSlot = this.anvillib$findCreativeSlot(mouseX, mouseY);
        if (overlay != null) {
            this.anvillib$closeVariantOverlay();
            if (button == 1 && sourceSlot == overlay.sourceSlot()) {
                this.anvillib$consumeMouseButton(button);
                cir.setReturnValue(true);
                return;
            }
        }
        if (button != 1 || sourceSlot == null
            || selectedTab == null || selectedTab.getType() != CreativeModeTab.Type.CATEGORY) return;
        CreativeVariantPickerOverlay.create(sourceSlot).ifPresent(created -> {
            this.anvillib$variantOverlay = created;
            this.anvillib$hoveredPickerVariant = null;
            this.anvillib$consumeMouseButton(button);
            cir.setReturnValue(true);
        });
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void anvillib$finishVariantOverlayClick(
        double mouseX,
        double mouseY,
        int button,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!this.anvillib$isMouseButtonConsumed(button)) return;
        this.anvillib$releaseMouseButton(button);
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void anvillib$blockVariantOverlayDrag(
        double mouseX,
        double mouseY,
        int button,
        double dragX,
        double dragY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (this.anvillib$isMouseButtonConsumed(button)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"))
    private void anvillib$closeVariantOverlayOnScroll(
        double mouseX,
        double mouseY,
        double scrollX,
        double scrollY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        this.anvillib$closeVariantOverlay();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void anvillib$handleVariantOverlayKey(
        int keyCode,
        int scanCode,
        int modifiers,
        CallbackInfoReturnable<Boolean> cir
    ) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (keyCode == InputConstants.KEY_ESCAPE
            || this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            this.anvillib$closeVariantOverlay();
            return;
        }
        CreativeVariantPickerOverlay overlay = this.anvillib$validVariantOverlay();
        ItemStack variant = this.anvillib$hoveredPickerVariant;
        if (overlay == null || variant == null) return;
        if (this.anvillib$handlePickerHotbarKey(overlay, variant, key)) {
            cir.setReturnValue(true);
            return;
        }
        if (this.minecraft.options.keyPickItem.isActiveAndMatches(key)) {
            this.anvillib$clickPickerVariant(overlay, variant, 0, ClickType.CLONE);
            cir.setReturnValue(true);
            return;
        }
        if (!this.minecraft.options.keyDrop.isActiveAndMatches(key)) return;
        this.anvillib$clickPickerVariant(
            overlay,
            variant,
            hasControlDown() ? 1 : 0,
            ClickType.THROW
        );
        cir.setReturnValue(true);
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void anvillib$clearVariantOverlay(CallbackInfo ci) {
        this.anvillib$closeVariantOverlay();
        this.anvillib$consumedMouseButtons = 0;
    }

    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
    private void anvillib$hideCoveredTabTooltip(
        GuiGraphics graphics,
        CreativeModeTab tab,
        int mouseX,
        int mouseY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        CreativeVariantPickerOverlay overlay = this.anvillib$validVariantOverlay();
        if (overlay != null && overlay.contains(this.leftPos, this.topPos, mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    @Nullable
    private CreativeVariantPickerOverlay anvillib$validVariantOverlay() {
        CreativeVariantPickerOverlay overlay = this.anvillib$variantOverlay;
        if (overlay == null) return null;
        if (selectedTab == null || selectedTab.getType() != CreativeModeTab.Type.CATEGORY || !overlay.isValid()) {
            this.anvillib$closeVariantOverlay();
            return null;
        }
        return overlay;
    }

    @Unique
    @Nullable
    private Slot anvillib$findCreativeSlot(double mouseX, double mouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.container != CONTAINER || !slot.isActive()) continue;
            double slotLeft = this.leftPos + slot.x - 1.0D;
            double slotTop = this.topPos + slot.y - 1.0D;
            if (mouseX >= slotLeft && mouseX < slotLeft + 18.0D
                && mouseY >= slotTop && mouseY < slotTop + 18.0D) {
                return slot;
            }
        }
        return null;
    }

    @Unique
    private void anvillib$selectPickerVariant(
        CreativeVariantPickerOverlay overlay,
        ItemStack variant,
        int button
    ) {
        ClickType clickType = hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
        this.anvillib$clickPickerVariant(overlay, variant, button, clickType);
    }

    @Unique
    private boolean anvillib$handlePickerHotbarKey(
        CreativeVariantPickerOverlay overlay,
        ItemStack variant,
        InputConstants.Key key
    ) {
        if (!this.menu.getCarried().isEmpty()) return false;
        if (this.minecraft.options.keySwapOffhand.isActiveAndMatches(key)) {
            this.anvillib$clickPickerVariant(overlay, variant, 40, ClickType.SWAP);
            return true;
        }
        for (int index = 0; index < this.minecraft.options.keyHotbarSlots.length; index++) {
            if (this.minecraft.options.keyHotbarSlots[index].isActiveAndMatches(key)) {
                this.anvillib$clickPickerVariant(overlay, variant, index, ClickType.SWAP);
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean anvillib$isPickerCloneMouseButton(int button) {
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(button);
        return this.menu.getCarried().isEmpty()
            && this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)
            && this.minecraft.player != null
            && this.minecraft.player.hasInfiniteMaterials();
    }

    @Unique
    private void anvillib$clickPickerHotbarMouseButton(
        CreativeVariantPickerOverlay overlay,
        ItemStack variant,
        int button
    ) {
        if (!this.menu.getCarried().isEmpty()) return;
        if (this.minecraft.options.keySwapOffhand.matchesMouse(button)) {
            this.anvillib$clickPickerVariant(overlay, variant, 40, ClickType.SWAP);
            return;
        }
        for (int index = 0; index < this.minecraft.options.keyHotbarSlots.length; index++) {
            if (this.minecraft.options.keyHotbarSlots[index].matchesMouse(button)) {
                this.anvillib$clickPickerVariant(overlay, variant, index, ClickType.SWAP);
            }
        }
    }

    @Unique
    private void anvillib$clickPickerVariant(
        CreativeVariantPickerOverlay overlay,
        ItemStack variant,
        int button,
        ClickType clickType
    ) {
        Slot sourceSlot = overlay.sourceSlot();
        ItemStack original = sourceSlot.getItem();
        sourceSlot.set(variant);
        try {
            this.slotClicked(sourceSlot, sourceSlot.index, button, clickType);
        } finally {
            sourceSlot.set(original);
        }
    }

    @Unique
    private void anvillib$closeVariantOverlay() {
        this.anvillib$variantOverlay = null;
        this.anvillib$hoveredPickerVariant = null;
    }

    @Unique
    private void anvillib$consumeMouseButton(int button) {
        if (button >= 0 && button < Integer.SIZE) {
            this.anvillib$consumedMouseButtons |= 1 << button;
        }
    }

    @Unique
    private boolean anvillib$isMouseButtonConsumed(int button) {
        return button >= 0 && button < Integer.SIZE
            && (this.anvillib$consumedMouseButtons & 1 << button) != 0;
    }

    @Unique
    private void anvillib$releaseMouseButton(int button) {
        if (button >= 0 && button < Integer.SIZE) {
            this.anvillib$consumedMouseButtons &= ~(1 << button);
        }
    }

    @Unique
    private int anvillib$scrollRow() {
        int rowCount = (this.menu.items.size() + anvillib$COLUMN_COUNT - 1) / anvillib$COLUMN_COUNT
            - anvillib$VISIBLE_ROW_COUNT;
        return Math.max((int) (this.scrollOffs * rowCount + 0.5F), 0);
    }

    @Unique
    private void anvillib$renderSection(
        GuiGraphics graphics,
        CreativeTabSections.PlacedSection placedSection,
        int scrollRow,
        int mouseX,
        int mouseY
    ) {
        CreativeTabSection section = placedSection.section();
        int row = placedSection.itemIndex() / anvillib$COLUMN_COUNT - scrollRow;
        if (row < 0 || row >= anvillib$VISIBLE_ROW_COUNT) return;
        int bannerWidth = section.bannerLength() * anvillib$CELL_SIZE;
        int bannerX = this.leftPos + anvillib$GRID_LEFT - 1;
        int bannerY = this.topPos + anvillib$GRID_TOP + row * anvillib$CELL_SIZE - 1;
        boolean hovered = mouseX >= bannerX && mouseX < bannerX + bannerWidth
            && mouseY >= bannerY && mouseY < bannerY + anvillib$CELL_SIZE;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0.0F, 0.0F, anvillib$BANNER_Z);
            graphics.blit(
                net.minecraft.client.renderer.RenderType::guiTextured,
                section.bannerTexture(),
                bannerX,
                bannerY,
                0,
                0,
                bannerWidth,
                anvillib$CELL_SIZE,
                bannerWidth,
                anvillib$CELL_SIZE
            );
            this.anvillib$renderBannerText(graphics, section, bannerX, bannerY, bannerWidth, hovered);
        } finally {
            graphics.pose().popPose();
        }
        if (hovered) {
            this.anvillib$hoveredSection = section;
        }
    }

    @Unique
    private void anvillib$renderBannerText(
        GuiGraphics graphics,
        CreativeTabSection section,
        int bannerX,
        int bannerY,
        int bannerWidth,
        boolean hovered
    ) {
        int textLeft = bannerX + section.textStart();
        int textRight = bannerX + section.textEnd();
        int maxTextWidth = section.textWidth();
        int textWidth = this.font.width(section.text());
        if (textWidth == 0) return;
        boolean overflowing = textWidth > maxTextWidth;
        FormattedCharSequence textToRender = section.text().getVisualOrderText();
        int visibleTextWidth = textWidth;
        if (!hovered && overflowing) {
            textToRender = this.font.split(section.text(), maxTextWidth).stream()
                .findFirst()
                .orElse(FormattedCharSequence.EMPTY);
            visibleTextWidth = this.font.width(textToRender);
        }
        int textX = switch (section.textAlignment()) {
            case LEFT -> textLeft;
            case CENTER -> textLeft + (maxTextWidth - visibleTextWidth) / 2;
            case RIGHT -> textRight - visibleTextWidth;
        };
        int textY = bannerY + (anvillib$CELL_SIZE - this.font.lineHeight) / 2 + 1;
        if ((section.textBackgroundColor() >>> 24) != 0) {
            boolean defaultTextRange = section.hasDefaultTextRange();
            int backgroundLeft;
            int backgroundRight;
            if (overflowing) {
                backgroundLeft = defaultTextRange ? bannerX : textLeft;
                backgroundRight = defaultTextRange ? bannerX + bannerWidth : textRight;
            } else if (defaultTextRange) {
                backgroundLeft = textX - anvillib$TEXT_PADDING;
                backgroundRight = textX + textWidth + anvillib$TEXT_PADDING;
            } else {
                backgroundLeft = Math.max(textLeft, textX - anvillib$TEXT_PADDING);
                backgroundRight = Math.min(textRight, textX + textWidth + anvillib$TEXT_PADDING);
            }
            graphics.fill(
                backgroundLeft,
                textY - 1,
                backgroundRight,
                textY + this.font.lineHeight,
                section.textBackgroundColor()
            );
        }
        if (hovered && overflowing) {
            AbstractWidget.renderScrollingString(
                graphics,
                this.font,
                section.text(),
                textLeft,
                bannerY,
                textRight,
                bannerY + anvillib$CELL_SIZE,
                0xFFFFFFFF
            );
            return;
        }
        graphics.drawString(this.font, textToRender, textX, textY, 0xFFFFFFFF, true);
    }
}
