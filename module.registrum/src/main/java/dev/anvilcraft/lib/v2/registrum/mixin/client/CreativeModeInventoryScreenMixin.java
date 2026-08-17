package dev.anvilcraft.lib.v2.registrum.mixin.client;

import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSection;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin
    extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Unique
    private static final int anvillib$COLUMN_COUNT = 9;
    @Unique
    private static final int anvillib$VISIBLE_ROW_COUNT = 5;
    @Unique
    private static final int anvillib$CELL_SIZE = 18;
    @Unique
    private static final int anvillib$GRID_LEFT = 9;
    @Unique
    private static final int anvillib$GRID_TOP = 18;
    @Unique
    private static final int anvillib$BANNER_Z = 200;
    @Unique
    private static final int anvillib$TEXT_PADDING = 2;

    @Shadow
    private static CreativeModeTab selectedTab;
    @Shadow
    private float scrollOffs;

    @Unique
    private CreativeTabSection anvillib$hoveredSection;

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
                + "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            shift = At.Shift.AFTER
        )
    )
    private void anvillib$renderSectionTooltip(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick,
        CallbackInfo ci
    ) {
        if (this.anvillib$hoveredSection == null) return;
        List<Component> tooltip = this.anvillib$hoveredSection.tooltip();
        if (!tooltip.isEmpty()) graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
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
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0.0F, 0.0F, anvillib$BANNER_Z);
            graphics.blit(
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
            this.anvillib$renderBannerText(graphics, section, bannerX, bannerY, bannerWidth);
        } finally {
            graphics.pose().popPose();
        }
        if (mouseX >= bannerX && mouseX < bannerX + bannerWidth
            && mouseY >= bannerY && mouseY < bannerY + anvillib$CELL_SIZE) {
            this.anvillib$hoveredSection = section;
        }
    }

    @Unique
    private void anvillib$renderBannerText(
        GuiGraphics graphics,
        CreativeTabSection section,
        int bannerX,
        int bannerY,
        int bannerWidth
    ) {
        int maxTextWidth = bannerWidth - anvillib$TEXT_PADDING * 2;
        int textWidth = this.font.width(section.text());
        if (textWidth == 0) return;
        float textScale = Math.min(1.0F, (float) maxTextWidth / textWidth);
        int scaledTextWidth = (int) Math.ceil(textWidth * textScale);
        int scaledTextHeight = (int) Math.ceil(this.font.lineHeight * textScale);
        int textX = bannerX + (bannerWidth - scaledTextWidth) / 2;
        int textY = bannerY + (anvillib$CELL_SIZE - scaledTextHeight) / 2 + 1;
        if ((section.textBackgroundColor() >>> 24) != 0) {
            graphics.fill(
                textX - anvillib$TEXT_PADDING,
                textY - 1,
                textX + scaledTextWidth + anvillib$TEXT_PADDING,
                textY + scaledTextHeight,
                section.textBackgroundColor()
            );
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(textX, textY, 0.0F);
            graphics.pose().scale(textScale, textScale, 1.0F);
            graphics.drawString(this.font, section.text(), 0, 0, 0xFFFFFFFF, true);
        } finally {
            graphics.pose().popPose();
        }
    }
}
