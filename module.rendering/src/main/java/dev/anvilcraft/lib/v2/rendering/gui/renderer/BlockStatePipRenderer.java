package dev.anvilcraft.lib.v2.rendering.gui.renderer;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.ALRSharedMath;
import dev.anvilcraft.lib.v2.rendering.util.Timer;
import dev.anvilcraft.lib.v2.rendering.foundation.fakeworld.SimpleDelegatingTintAccess;
import dev.anvilcraft.lib.v2.rendering.foundation.fakeworld.SimpleTintedEmptyLevelAccess;
import dev.anvilcraft.lib.v2.rendering.gui.state.BlockStatePipRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import dev.anvilcraft.lib.v2.rendering.foundation.FeatureGeometry;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;


@ApiStatus.Internal
public class BlockStatePipRenderer extends PictureInPictureRenderer<BlockStatePipRenderingState> {

    @Override
    public Class<BlockStatePipRenderingState> getRenderStateClass() {
        return BlockStatePipRenderingState.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void renderToTexture(BlockStatePipRenderingState renderState, PoseStack poseStack, SubmitNodeCollector collector) {
        BlockAndTintGetter level = renderState.level();
        BlockPos blockPos = renderState.blockPos();
        BlockEntity blockEntity = null;
        BlockEntityRenderer blockEntityRenderer = null;
        Minecraft minecraft = Minecraft.getInstance();
        long seed = 42;
        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(renderState.ambientOcclusion(), true, minecraft.getBlockColors());
        BlockState state = renderState.blockState();
        BlockAndTintGetter wrappedLevel;
        if (level != null && blockPos != null) {
            blockEntity = level.getBlockEntity(blockPos);
            blockEntityRenderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            seed = state.getSeed(blockPos);
            wrappedLevel = new SimpleDelegatingTintAccess(level);
        }else {
            wrappedLevel = new SimpleTintedEmptyLevelAccess();
        }

        int guiScale = minecraft.gameRenderer.gameRenderState().windowRenderState.guiScale;
        float width = (renderState.fx1() - renderState.fx0()) * guiScale;
        float height = (renderState.fy1() - renderState.fy0()) * guiScale;
        float scale = guiScale * renderState.scale();

        poseStack.setIdentity();

        poseStack.translate(width / 2f, height / 2f, 0);
        poseStack.scale(1,-1,1);
        poseStack.translate(-width / 2f, -height / 2f, 0);

        poseStack.scale(scale, scale, scale);

        poseStack.translate((ALRSharedMath.SQRT_2 / 4) + 0.5, 0.5, 0);

        poseStack.last().pose().mul(renderState.pose3D().pose());
        poseStack.last().normal().mul(renderState.pose3D().normal());
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        blockRenderer.tesselateBlock(
            (x, y, z, quad, instance) -> {
                poseStack.pushPose();
                poseStack.translate(x, y, z);
                poseStack.translate(-0.5f, 0, -0.5f);
                FeatureGeometry.submitQuad(collector, poseStack, quad, instance);
                poseStack.popPose();
            },
            0,
            0,
            0,
            wrappedLevel,
            BlockPos.ZERO,
            state,
            minecraft.getModelManager().getBlockStateModelSet().get(state),
            seed
        );
        if (blockEntity != null && blockEntityRenderer != null) {
            GameRenderer gameRenderer = minecraft.gameRenderer;
            poseStack.pushPose();
            poseStack.translate(-0.5f, 0, -0.5f);
            BlockEntityRenderState blockEntityRenderState = blockEntityRenderer.createRenderState();
            blockEntityRenderer.extractRenderState(blockEntity, blockEntityRenderState, Timer.getPartialTick(), Vec3.ZERO, null);
            blockEntityRenderer.submit(
                blockEntityRenderState,
                poseStack,
                collector,
                gameRenderer.gameRenderState().levelRenderState.cameraRenderState
            );
            poseStack.popPose();
        }
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 4f;
    }

    @Override
    protected String getTextureLabel() {
        return "block state";
    }

}
