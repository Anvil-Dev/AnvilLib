package dev.anvilcraft.lib.v2.wheel.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface WheelEntryRenderer {
    void render(GuiGraphics graphics, PoseStack pose, int width, int height);
}

