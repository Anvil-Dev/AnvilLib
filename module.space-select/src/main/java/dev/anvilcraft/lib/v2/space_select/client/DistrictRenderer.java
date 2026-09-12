package dev.anvilcraft.lib.v2.space_select.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.space_select.AnvilLibSpaceSelect;
import dev.anvilcraft.lib.v2.space_select.District;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilLibSpaceSelect.MOD_ID, value = Dist.CLIENT)
@ApiStatus.Internal
public class DistrictRenderer {
    @SubscribeEvent
    public static void addLevelRenderMainPass(RenderLevelStageEvent.AfterParticles event) {
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        renderDistrict(bufferSource, poseStack, event.getCamera().getPosition());
    }

    public static void renderDistrict(
        MultiBufferSource.BufferSource bufferSource,
        PoseStack poseStack,
        Vec3 cameraPos
    ) {
        List<District> districts = new ArrayList<>(AnvilLibSpaceSelectClient.MANAGER.getDistrictMap().values());
        District tempDistrict = AnvilLibSpaceSelectClient.MANAGER.getTempDistrict();
        if (tempDistrict != null) districts.add(tempDistrict);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        for (District district : districts) {
            BlockPos pos = district.start();
            double dx = (double) pos.getX() - cameraPos.x;
            double dy = (double) pos.getY() - cameraPos.y;
            double dz = (double) pos.getZ() - cameraPos.z;
            if (SharedConstants.DEBUG_SHAPES) {
                AABB box = district.shape().bounds().move(dx, dy, dz);
                ShapeRenderer.renderLineBox(poseStack, buffer, box, 1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                int color = district.color();
                float r = (float) (color >> 16 & 0xFF) / 255.0F;
                float g = (float) (color >> 8 & 0xFF) / 255.0F;
                float b = (float) (color & 0xFF) / 255.0F;
                float a = (float) (color >> 24 & 0xFF) / 255.0F;
                ShapeRenderer.renderShape(poseStack, buffer, district.shape(), dx, dy, dz, color);
            }
        }
        bufferSource.endLastBatch();
    }
}
