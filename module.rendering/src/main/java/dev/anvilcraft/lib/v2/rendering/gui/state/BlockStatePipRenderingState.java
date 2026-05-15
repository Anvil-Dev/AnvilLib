package dev.anvilcraft.lib.v2.rendering.gui.state;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record BlockStatePipRenderingState(
    BlockState blockState,
    @Nullable Level level,
    @Nullable BlockPos blockPos,
    int x0,
    int y0,
    int x1,
    int y1,
    int color,
    boolean ambientOcclusion,
    PoseStack.Pose pose3D,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public static final PoseStack.Pose IDENTITY_POSE_3D = new PoseStack.Pose();
    private static final float SQRT_2 = 1.4142135623730950488016887242097f;

    static {
        IDENTITY_POSE_3D.setIdentity();
    }

    public BlockStatePipRenderingState(
        BlockState blockState,
        @Nullable Level level,
        @Nullable BlockPos blockPos,
        int x0,
        int y0,
        int x1,
        int y1,
        int color,
        boolean ambientOcclusion,
        PoseStack.Pose pose3D,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(
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
            pose,
            scissorArea,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea)
        );
    }

    @Override
    public float scale() {
        int width = x1 - x0;
        int height = y1 - y0;
        return Math.min(width / 2f, height / 2f);
    }
}