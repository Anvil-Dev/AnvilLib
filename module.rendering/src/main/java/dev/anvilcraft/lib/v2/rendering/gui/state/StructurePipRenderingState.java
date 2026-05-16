package dev.anvilcraft.lib.v2.rendering.gui.state;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record StructurePipRenderingState (
    BlockAndTintGetter structureAccess,
    BlockPos startPos,
    BlockPos endPos,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    boolean ambientOcclusion,
    PoseStack.Pose pose3D,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public StructurePipRenderingState(
        BlockAndTintGetter structureAccess,
        BlockPos startPos,
        BlockPos endPos,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        boolean ambientOcclusion,
        PoseStack.Pose pose3D,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea
    ){
        this(
            structureAccess,
            startPos,
            endPos,
            x0,
            y0,
            x1,
            y1,
            scale,
            ambientOcclusion,
            pose3D,
            pose,
            scissorArea,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea)
        );
    }
}
