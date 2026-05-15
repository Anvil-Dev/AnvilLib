package dev.anvilcraft.lib.v2.test.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class GuiTestScreen extends Screen {
    public GuiTestScreen() {
        super(Component.literal("SCREEN TEST"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.pose().pushMatrix().scale(1);
        int startX = 10;
        int startY = 30;

        for (int ix = 0, x = startX; ix < 10; ix++, x += 16 + 2) {
            graphics.text(
                Minecraft.getInstance().font,
                String.valueOf(((ix + 1) / 10f)),
                x,
                startY + 16 * -1,
                -1
            );
        }

        for (int ix = 0, x = startX; ix < 10; ix++, x += 16 + 2) {
            GuiRenderExtras.itemWithTransparency(
                graphics,
                Items.BEACON.getDefaultInstance(),
                x,
                startY + 16 * 0,
                (ix + 1) / 10f
            );
        }
        for (int ix = 0, x = startX; ix < 10; ix++, x += 16 + 2) {
            GuiRenderExtras.itemWithTransparency(
                graphics,
                Items.LIME_STAINED_GLASS.getDefaultInstance(),
                x,
                startY + 16 * 1,
                (ix + 1) / 10f
            );
        }
        for (int ix = 0, x = startX; ix < 10; ix++, x += 16 + 2) {
            GuiRenderExtras.itemWithTransparency(
                graphics,
                Items.CHEST.getDefaultInstance(),
                x,
                startY + 16 * 2,
                (ix + 1) / 10f
            );
        }
        for (int ix = 0, x = startX; ix < 10; ix++, x += 16 + 2) {
            GuiRenderExtras.itemWithTransparency(
                graphics,
                Items.DIAMOND.getDefaultInstance(),
                x,
                startY + 16 * 3,
                (ix + 1) / 10f
            );
        }

        float gameTime = (minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        int size = (int) ((Math.sin(gameTime * 0.25) + 1.25f) * 4f * 32f);

        PoseStack poseStack = new PoseStack();

        poseStack.mulPose(Axis.XP.rotationDegrees(30));
        poseStack.mulPose(Axis.YP.rotationDegrees(45));

        poseStack.mulPose(Axis.YP.rotationDegrees(gameTime* 4.25f));


        graphics.fill(
            startX,
            startY + 16 * 4,
            startX + size,
            startY + 16 * 4 + size,
            -1
        );
        GuiRenderExtras.tessellateBlock(
            graphics,
            Blocks.GLASS.defaultBlockState(),
            null,
            null,
            startX,
            startY + 16 * 4,
            size,
            true,
            poseStack
        );
        graphics.pose().popMatrix();
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
