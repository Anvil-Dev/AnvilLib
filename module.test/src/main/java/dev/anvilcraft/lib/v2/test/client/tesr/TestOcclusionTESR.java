package dev.anvilcraft.lib.v2.test.client.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import dev.anvilcraft.lib.v2.rendering.ALROptimizations;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionCuller;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionKey;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionSubmitNodeStorage;
import dev.anvilcraft.lib.v2.test.block.tile.TestOcclusionTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestOcclusionTESR implements BlockEntityRenderer<TestOcclusionTile, TestOcclusionTESR.RenderState> {

    public TestOcclusionTESR(BlockEntityRendererProvider.Context ignored) {
    }


    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
        TestOcclusionTile blockEntity,
        RenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.occlusionKey = blockEntity.getOcclusionKey();
    }

    @Override
    public void submit(
        RenderState renderState,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState
    ) {
        OcclusionCuller occlusionCuller = ALROptimizations.getOcclusionCuller();
        OcclusionSubmitNodeStorage wrapped = occlusionCuller.wrapSubmitNodeStorage(submitNodeCollector);
        Minecraft minecraft = Minecraft.getInstance();

        wrapped.beginOcclusionRecord(renderState.occlusionKey);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        ItemStackRenderState isrs = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(
            isrs,
            Items.CARROT.getDefaultInstance(),
            ItemDisplayContext.FIXED,
            minecraft.level,
            minecraft.player,
            42
        );
        isrs.submit(poseStack, wrapped, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        wrapped.endOcclusionRecord();
    }


    public static class RenderState extends BlockEntityRenderState {
        private OcclusionKey occlusionKey;
    }
}
