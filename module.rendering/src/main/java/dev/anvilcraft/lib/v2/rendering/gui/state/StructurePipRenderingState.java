package dev.anvilcraft.lib.v2.rendering.gui.state;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

@ApiStatus.Internal
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
    boolean glitched,
    PoseStack.Pose pose3D,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds,
    @Nullable BiConsumer<SubmitNodeCollector, PoseStack> drawAdditionalCallback
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
        boolean glitched,
        PoseStack.Pose pose3D,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable BiConsumer<SubmitNodeCollector, PoseStack> drawAdditionalCallback
    ) {
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
            glitched,
            pose3D,
            pose,
            scissorArea,
            PictureInPictureRenderState.getBounds(Mth.floor(x0), Mth.floor(y0), Mth.floor(x1), Mth.floor(y1), scissorArea),
            drawAdditionalCallback
        );
    }

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
        boolean glitched,
        PoseStack.Pose pose3D,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea
    ) {
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
            glitched,
            pose3D,
            pose,
            scissorArea,
            PictureInPictureRenderState.getBounds(Mth.floor(x0), Mth.floor(y0), Mth.floor(x1), Mth.floor(y1), scissorArea),
            null
        );
    }

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
        boolean glitched,
        PoseStack.Pose pose3D,
        Matrix3x2f pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
    ) {
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
            glitched,
            pose3D,
            pose,
            scissorArea,
            bounds,
            null
        );
    }
}
