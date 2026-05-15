package dev.anvilcraft.lib.v2.rendering.gui;

import dev.anvilcraft.lib.v2.rendering.extension.GuiGraphicsExtractorExtension;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public class GuiRenderHelper {

    public static void itemWithTransparency(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack stack, int x, int y, float alpha) {
        GuiGraphicsExtractorExtension.of(guiGraphicsExtractor).translucentItem(stack, x, y, alpha);
    }
}
