package dev.anvilcraft.lib.v2.test.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.foundation.fakeworld.FakeDisplayLevel;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class GuiTestScreen extends Screen {

    private final FakeDisplayLevel level = new FakeDisplayLevel(Minecraft.getInstance().level);
    private final FakeDisplayLevel structure = new FakeDisplayLevel(Minecraft.getInstance().level);

    public GuiTestScreen() {
        super(Component.literal("SCREEN TEST"));
        BlockState chestState = Blocks.CHEST.defaultBlockState();
        level.setBlockEntity(BlockPos.ZERO, new ChestBlockEntity(BlockPos.ZERO, chestState));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                structure.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 0);
            }
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean isEdge = x == -2 || x == 2 || z == -2 || z == 2;
                if (isEdge) {
                    structure.setBlock(new BlockPos(x, 1, z), Blocks.GLASS.defaultBlockState(), 0);
                } else {
                    structure.setFluidState(new BlockPos(x, 1, z), Fluids.WATER.defaultFluidState());
                }
            }
        }
        BlockPos chestPos = new BlockPos(0, 2, 0);
        structure.setBlock(chestPos, chestState, 0);
        structure.setBlockEntity(chestPos, new ChestBlockEntity(chestPos, chestState));
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

        int size = 144;
        graphics.fill(
            startX,
            startY + 16 * 4,
            startX + size,
            startY + 16 * 4 + size,
            0x4429B6F6
        );

        graphics.fill(
            startX,
            startY + 16 * 4 + size,
            startX + size,
            startY + 16 * 4 + size + size,
            0x44DCE775
        );

        graphics.fill(
            startX + 144,
            startY + 16 * 4,
            startX + 144 + 144,
            startY + 16 * 4 + 144,
            0x44FF8A65
        );

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(30));
        poseStack.mulPose(Axis.YP.rotationDegrees(45));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(gameTime * 4.25f));

        GuiRenderExtras.tessellateBlock(
            graphics,
            Blocks.CHEST.defaultBlockState(),
            level,
            BlockPos.ZERO,
            startX,
            startY + 16 * 4,
            size,
            true,
            poseStack
        );

        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-gameTime * 4.25f));
        GuiRenderExtras.tessellateBlock(
            graphics,
            Blocks.GRASS_BLOCK.defaultBlockState(),
            null,
            null,
            startX,
            startY + 16 * 4 + size,
            size,
            true,
            poseStack
        );

        poseStack.popPose();

        poseStack.popPose();

        poseStack.pushPose();

        poseStack.mulPose(Axis.XP.rotationDegrees(30));
        poseStack.mulPose(Axis.YP.rotationDegrees(45));
        poseStack.mulPose(Axis.YP.rotationDegrees(gameTime * 4.25f));

        GuiRenderExtras.submitStructure(
            graphics,
            structure,
            new BlockPos(-2, 0, -2),
            new BlockPos(2, 2, 2),
            startX + 144,
            startY + 16 * 4,
            startX + 144 + 144,
            startY + 16 * 4 + 144,
            18,
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
