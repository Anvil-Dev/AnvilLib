package dev.anvilcraft.lib.v2.test.client.tesr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import dev.anvilcraft.lib.v2.rendering.foundation.compound.CompoundSubmitNodeStorage;
import dev.anvilcraft.lib.v2.test.block.tile.TestBloomTile;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestBloomTESR implements BlockEntityRenderer<TestBloomTile, TestBloomTESR.TestBloomRenderState> {

    public TestBloomTESR(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TestBloomRenderState createRenderState() {
        return new TestBloomRenderState();
    }

    @Override
    public void extractRenderState(
        TestBloomTile blockEntity,
        TestBloomRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.setText(blockEntity.aaa.getValue().toString());
    }

    @Override
    public void submit(
        TestBloomRenderState testBloomRenderState,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState
    ) {
        CompoundSubmitNodeStorage compoundSubmit = ALRPostEffects.getBloomPostEffect().createCompoundSubmitStorage(submitNodeCollector);
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        poseStack.translate(0, 2, 0);
        poseStack.scale(1 / 16f, -1 / 16f, 1 / 16f);
        MutableComponent literal = Component.literal(testBloomRenderState.getText());
        compoundSubmit.submitText(
            poseStack,
            0,
            0,
            literal.getVisualOrderText(),
            false,
            Font.DisplayMode.NORMAL,
            LightCoordsUtil.FULL_BRIGHT,
            0xFFFFFFFF,
            0,
            0
        );

        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(
            Axis.YP.rotationDegrees(
                (minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)) * 2.25f
            )
        );
        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(
            renderState,
            Items.CARROT.getDefaultInstance(),
            ItemDisplayContext.FIXED,
            minecraft.level,
            minecraft.player,
            42
        );
        renderState.submit(poseStack, compoundSubmit, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static class TestBloomRenderState extends BlockEntityRenderState {
        @Setter
        @Getter
        private String text = "";
    }
}
