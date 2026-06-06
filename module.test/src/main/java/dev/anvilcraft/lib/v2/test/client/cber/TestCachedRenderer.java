package dev.anvilcraft.lib.v2.test.client.cber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderState;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderer;
import dev.anvilcraft.lib.v2.rendering.foundation.BloomSubmitNodeStorage;
import dev.anvilcraft.lib.v2.test.block.tile.TestCachedRenderingTile;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;

public class TestCachedRenderer implements CachedBlockEntityRenderer<TestCachedRenderingTile, TestCachedRenderer.State> {

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public State extractRenderState(TestCachedRenderingTile blockEntity, State state, float partialTicks, Camera camera) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockEntityRenderState.extractBase(blockEntity, state, null);
        minecraft.getItemModelResolver().updateForTopItem(
            state.renderState,
            Items.CARROT.getDefaultInstance(),
            ItemDisplayContext.FIXED,
            minecraft.level,
            minecraft.player,
            42
        );
        minecraft.getBlockModelResolver().update(
            state.blockModelRenderState,
            Blocks.SEA_LANTERN.defaultBlockState(),
            state.displayContext
        );

        minecraft.getBlockModelResolver().update(
            state.blockModelRenderState1,
            Blocks.LIME_STAINED_GLASS.defaultBlockState(),
            state.displayContext
        );

        return state;
    }

    @Override
    public void submit(State renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        renderState.renderState.submit(
            poseStack,
            submitNodeCollector,
            LightCoordsUtil.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            0
        );
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 1.5, 0);
        renderState.blockModelRenderState.submit(
            poseStack,
            BloomSubmitNodeStorage.wrap(submitNodeCollector),
            LightCoordsUtil.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            0
        );

        poseStack.translate(0, 1, 0);
        renderState.blockModelRenderState1.submit(
            poseStack,
            submitNodeCollector,
            LightCoordsUtil.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            0
        );
        poseStack.popPose();
    }

    public static class State extends CachedBlockEntityRenderState {
        private final ItemStackRenderState renderState = new ItemStackRenderState();
        private final BlockModelRenderState blockModelRenderState = new BlockModelRenderState();
        private final BlockModelRenderState blockModelRenderState1 = new BlockModelRenderState();
        private final BlockDisplayContext displayContext = BlockDisplayContext.create();
    }
}
