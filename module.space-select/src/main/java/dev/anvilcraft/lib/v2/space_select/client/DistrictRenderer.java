package dev.anvilcraft.lib.v2.space_select.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.space_select.AnvilLibSpaceSelect;
import dev.anvilcraft.lib.v2.space_select.District;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilLibSpaceSelect.MOD_ID, value = Dist.CLIENT)
public class DistrictRenderer {
    @SubscribeEvent
    public static void addLevelRenderMainPass(RenderLevelStageEvent.AfterTranslucentParticles event) {
        LevelRenderState renderState = event.getLevelRenderState();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        renderDistrict(bufferSource, poseStack, renderState);
    }

    public static void renderDistrict(
        MultiBufferSource.BufferSource bufferSource,
        PoseStack poseStack,
        LevelRenderState levelRenderState
    ) {
        List<District> districts = new ArrayList<>(AnvilLibSpaceSelectClient.MANAGER.getDistrictMap().values());
        District tempDistrict = AnvilLibSpaceSelectClient.MANAGER.getTempDistrict();
        if (tempDistrict != null) districts.add(tempDistrict);
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.lines());
        for (District district : districts) {
            BlockOutlineRenderState state = new BlockOutlineRenderState(district.start(), false, false, district.shape(), List.of());
            DistrictRenderer.renderOutline(
                poseStack,
                buffer,
                cameraPos.x,
                cameraPos.y,
                cameraPos.z,
                state,
                district.color(),
                Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth
            );
        }
        bufferSource.endLastBatch();
    }

    private static void renderOutline(
        PoseStack poseStack,
        VertexConsumer builder,
        double camX,
        double camY,
        double camZ,
        BlockOutlineRenderState state,
        int color,
        float width
    ) {
        BlockPos pos = state.pos();
        if (SharedConstants.DEBUG_SHAPES) {
            ShapeRenderer.renderShape(
                poseStack,
                builder,
                state.shape(),
                (double) pos.getX() - camX,
                (double) pos.getY() - camY,
                (double) pos.getZ() - camZ,
                ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F),
                width
            );
            if (state.collisionShape() != null) {
                ShapeRenderer.renderShape(
                    poseStack,
                    builder,
                    state.collisionShape(),
                    (double) pos.getX() - camX,
                    (double) pos.getY() - camY,
                    (double) pos.getZ() - camZ,
                    ARGB.colorFromFloat(0.4F, 0.0F, 0.0F, 0.0F),
                    width
                );
            }

            if (state.occlusionShape() != null) {
                ShapeRenderer.renderShape(
                    poseStack,
                    builder,
                    state.occlusionShape(),
                    (double) pos.getX() - camX,
                    (double) pos.getY() - camY,
                    (double) pos.getZ() - camZ,
                    ARGB.colorFromFloat(0.4F, 0.0F, 1.0F, 0.0F),
                    width
                );
            }

            if (state.interactionShape() != null) {
                ShapeRenderer.renderShape(
                    poseStack,
                    builder,
                    state.interactionShape(),
                    (double) pos.getX() - camX,
                    (double) pos.getY() - camY,
                    (double) pos.getZ() - camZ,
                    ARGB.colorFromFloat(0.4F, 0.0F, 0.0F, 1.0F),
                    width
                );
            }
        } else {
            ShapeRenderer.renderShape(
                poseStack,
                builder,
                state.shape(),
                (double) pos.getX() - camX,
                (double) pos.getY() - camY,
                (double) pos.getZ() - camZ,
                color,
                width
            );
        }
    }
}
