package dev.anvilcraft.lib.v2.test.client.cber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderState;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderer;
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
            Blocks.LIME_STAINED_GLASS.defaultBlockState(),
            state.displayContext
        );

        return state;
    }

    @Override
    public void submit(State renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, boolean bloomed) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        renderState.renderState.submit(poseStack, submitNodeCollector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    poseStack.pushPose();
                    poseStack.translate(x, y, z);
                    renderState.blockModelRenderState.submit(poseStack, submitNodeCollector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                    poseStack.popPose();
                }
            }
        }
    }

    public static class State extends CachedBlockEntityRenderState {
        private final ItemStackRenderState renderState = new ItemStackRenderState();
        private final BlockModelRenderState blockModelRenderState = new BlockModelRenderState();
        private final BlockDisplayContext displayContext = BlockDisplayContext.create();
    }
}
