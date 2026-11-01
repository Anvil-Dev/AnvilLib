package dev.anvilcraft.lib.v2.space_select.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.space_select.AnvilLibSpaceSelect;
import dev.anvilcraft.lib.v2.space_select.District;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilLibSpaceSelect.MOD_ID, value = Dist.CLIENT)
@ApiStatus.Internal
public class DistrictRenderer {
    private static final ContextKey<List<DistrictOutline>> OUTLINES = new ContextKey<>(
        Identifier.fromNamespaceAndPath(AnvilLibSpaceSelect.MOD_ID, "district_outlines"));

    @SubscribeEvent
    public static void extractDistricts(ExtractLevelRenderStateEvent event) {
        List<District> districts = new ArrayList<>(AnvilLibSpaceSelectClient.MANAGER.getDistrictMap().values());
        District tempDistrict = AnvilLibSpaceSelectClient.MANAGER.getTempDistrict();
        if (tempDistrict != null) districts.add(tempDistrict);
        float width = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
        event.getRenderState().setRenderData(OUTLINES, districts.stream().map(district -> new DistrictOutline(
            district.start().immutable(), district.shape(), SharedConstants.DEBUG_SHAPES ? -1 : district.color(), width
        )).toList());
    }

    @SubscribeEvent
    public static void submitDistricts(SubmitCustomGeometryEvent event) {
        renderDistrict(event.getSubmitNodeCollector(), event.getPoseStack(), event.getLevelRenderState());
    }

    public static void renderDistrict(SubmitNodeCollector collector, PoseStack poses, LevelRenderState state) {
        List<DistrictOutline> outlines = state.getRenderData(OUTLINES);
        if (outlines == null) return;
        Vec3 camera = state.cameraRenderState.pos;
        for (DistrictOutline outline : outlines) {
            poses.pushPose();
            try {
                poses.translate(outline.pos().getX() - camera.x, outline.pos().getY() - camera.y, outline.pos().getZ() - camera.z);
                collector.submitShapeOutline(poses, outline.shape(), RenderTypes.lines(), outline.color(), outline.width(), true);
            } finally {
                poses.popPose();
            }
        }
    }

    private record DistrictOutline(BlockPos pos, VoxelShape shape, int color, float width) { }
}
