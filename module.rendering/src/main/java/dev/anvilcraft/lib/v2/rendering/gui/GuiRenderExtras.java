package dev.anvilcraft.lib.v2.rendering.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.extension.GuiGraphicsExtractorExtension;
import dev.anvilcraft.lib.v2.rendering.gui.state.BlockStatePipRenderingState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public class GuiRenderExtras {

    public static void itemWithTransparency(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack stack, int x, int y, float alpha) {
        GuiGraphicsExtractorExtension.of(guiGraphicsExtractor).translucentItem(stack, x, y, alpha);
    }

    public static void tessellateBlock(
        GuiGraphicsExtractor guiGraphicsExtractor,
        BlockState blockState,
        @Nullable Level level,
        @Nullable BlockPos blockPos,
        int x0,
        int y0,
        int x1,
        int y1,
        int color,
        boolean ambientOcclusion,
        PoseStack.Pose pose3D
    ) {
        guiGraphicsExtractor.submitPictureInPictureRenderState(
            new BlockStatePipRenderingState(
                blockState,
                level,
                blockPos,
                x0,
                y0,
                x1,
                y1,
                color,
                ambientOcclusion,
                pose3D,
                guiGraphicsExtractor.pose().get(new Matrix3x2f()),
                guiGraphicsExtractor.peekScissorStack()
            )
        );
    }

    public static void tessellateBlock(
        GuiGraphicsExtractor guiGraphicsExtractor,
        BlockState blockState,
        @Nullable Level level,
        @Nullable BlockPos blockPos,
        int x0,
        int y0,
        int x1,
        int y1,
        int color,
        boolean ambientOcclusion,
        PoseStack poseStack3D
    ) {
        tessellateBlock(guiGraphicsExtractor, blockState, level, blockPos, x0, y0, x1, y1, color, ambientOcclusion, poseStack3D.last());
    }

    public static void tessellateBlock(
        GuiGraphicsExtractor guiGraphicsExtractor,
        BlockState blockState,
        @Nullable Level level,
        @Nullable BlockPos blockPos,
        int x0,
        int y0,
        int x1,
        int y1,
        boolean ambientOcclusion,
        PoseStack poseStack3D
    ) {
        tessellateBlock(guiGraphicsExtractor, blockState, level, blockPos, x0, y0, x1, y1, -1, ambientOcclusion, poseStack3D);
    }

    public static void tessellateBlock(
        GuiGraphicsExtractor guiGraphicsExtractor,
        BlockState blockState,
        @Nullable Level level,
        @Nullable BlockPos blockPos,
        int x0,
        int y0,
        int width,
        boolean ambientOcclusion,
        PoseStack poseStack3D
    ) {
        tessellateBlock(guiGraphicsExtractor, blockState, level, blockPos, x0, y0, x0 + width, y0 + width, -1, ambientOcclusion, poseStack3D);
    }

    public static void tessellateBlock(
        GuiGraphicsExtractor guiGraphicsExtractor,
        BlockState blockState,
        @Nullable Level level,
        @Nullable BlockPos blockPos,
        int x0,
        int y0,
        boolean ambientOcclusion,
        PoseStack poseStack3D
    ) {
        tessellateBlock(guiGraphicsExtractor, blockState, level, blockPos, x0, y0, x0 + 32, y0 + 32, -1, ambientOcclusion, poseStack3D);
    }

    public static void tessellateBlock(
        GuiGraphicsExtractor guiGraphicsExtractor,
        BlockState blockState,
        int x0,
        int y0,
        PoseStack poseStack3D
    ) {
        tessellateBlock(guiGraphicsExtractor, blockState, null, null, x0, y0, x0 + 32, y0 + 32, -1, false, poseStack3D);
    }

    public static void tessellateBlock(
        GuiGraphicsExtractor guiGraphicsExtractor,
        BlockState blockState,
        int x0,
        int y0,
        boolean ambientOcclusion,
        PoseStack poseStack3D
    ) {
        tessellateBlock(guiGraphicsExtractor, blockState, null, null, x0, y0, x0 + 32, y0 + 32, -1, ambientOcclusion, poseStack3D);
    }

    public static void tessellateBlock(
        GuiGraphicsExtractor guiGraphicsExtractor,
        BlockState blockState,
        int x0,
        int y0
    ) {
        tessellateBlock(guiGraphicsExtractor, blockState, null, null, x0, y0, x0 + 32, y0 + 32, -1, false, BlockStatePipRenderingState.IDENTITY_POSE_3D);
    }
}
