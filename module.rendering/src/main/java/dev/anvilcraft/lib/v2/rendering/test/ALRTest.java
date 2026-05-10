package dev.anvilcraft.lib.v2.rendering.test;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;

public class ALRTest {


    public static void renderCarrotBloomed() {
        if (!AnvilLibRendering.DEBUG) return;
        renderCarrot();
        AnvilLibRendering.getBloomPostEffect().drawBloomed(ALRTest::submitCarrot);
    }

    private static void submitCarrot(SubmitNodeCollector nodeCollector, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.translate(-0.1, 0, 0.28);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        ItemStackRenderState renderState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
            renderState,
            Items.CARROT.getDefaultInstance(),
            ItemDisplayContext.FIXED,
            Minecraft.getInstance().level,
            Minecraft.getInstance().player,
            42
        );
        renderState.submit(poseStack, nodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static void renderCarrot() {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().level == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(-0.1, 0, 0.28);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        ItemStackRenderState renderState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
            renderState,
            Items.CARROT.getDefaultInstance(),
            ItemDisplayContext.FIXED,
            Minecraft.getInstance().level,
            Minecraft.getInstance().player,
            42
        );
        RenderBuffers renderBuffers = minecraft.renderBuffers();
        FeatureRenderDispatcher frd = new FeatureRenderDispatcher(
            new SubmitNodeStorage(),
            minecraft.getModelManager(),
            renderBuffers.bufferSource(),
            minecraft.getAtlasManager(),
            renderBuffers.outlineBufferSource(),
            renderBuffers.crumblingBufferSource(),
            minecraft.font,
            minecraft.gameRenderer.getGameRenderState()
        );
        renderState.submit(poseStack, frd.getSubmitNodeStorage(), 15728880, OverlayTexture.NO_OVERLAY, 0);
        frd.renderAllFeatures();
        renderBuffers.bufferSource().endBatch();
        poseStack.popPose();
    }
}
