package dev.anvilcraft.lib.v2.rendering.gui.state;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import static dev.anvilcraft.lib.v2.rendering.ALRSharedMath.SQRT_2;

@org.jetbrains.annotations.ApiStatus.Internal
public record BlockStatePipRenderingState(
    BlockState blockState,
    @Nullable BlockAndTintGetter level,
    @Nullable BlockPos blockPos,
    float fx0,
    float fy0,
    float fx1,
    float fy1,
    int color,
    boolean ambientOcclusion,
    PoseStack.Pose pose3D,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

    public BlockStatePipRenderingState(
        BlockState blockState,
        @Nullable BlockAndTintGetter level,
        @Nullable BlockPos blockPos,
        float x0,
        float y0,
        float x1,
        float y1,
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
            PictureInPictureRenderState.getBounds(Mth.floor(x0), Mth.floor(y0), Mth.floor(x1), Mth.floor(y1), scissorArea)
        );
    }

    @Override
    public int x0() {
        return Mth.floor(fx0);
    }

    @Override
    public int y0() {
        return Mth.floor(fy0);
    }

    @Override
    public int x1() {
        return Mth.floor(fx1);
    }

    @Override
    public int y1() {
        return Mth.floor(fy1);
    }

    @Override
    public float scale() {
        float width = fx1 - fx0;
        float height = fy1 - fy0;
        return Math.min(width / (1f + SQRT_2 / 2f), height / SQRT_2);
    }
}
