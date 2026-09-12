package dev.anvilcraft.lib.v2.cube.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.cube.AnvilLibCube;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.anvilcraft.lib.v2.cube.geometry.PackedOutline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import java.util.List;

@EventBusSubscriber(modid = AnvilLibCube.MOD_ID, value = Dist.CLIENT)
public final class CubeHighlightEvents {
    private CubeHighlightEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void highlight(ExtractBlockOutlineRenderStateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) return;
        BlockPos pos = event.getBlockPos();
        float partial = CubeSelection.frameFraction(event.getLevel(), minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        CubeSelection.Target target = CubeSelection.target(event.getLevel(), pos, event.getBlockState(), partial);
        if (target == null) return;
        List<SelectionPart> parts = target.parts();
        PackedOutline[] outlines = new PackedOutline[parts.size()];
        int segments = 0;
        for (int i = 0; i < outlines.length; i++) {
            outlines[i] = CubeSelection.outlines().get(parts.get(i).geometry());
            segments += outlines[i].segmentCount();
        }
        PackedOutline fallback = segments > SelectionGeometry.MAX_OUTLINE_SEGMENTS
            ? new SelectionGeometry(List.of(ConvexShape.box(target.bounds()))).fallbackOutline() : null;
        Vec3 offset = target.offset();
        float lineWidth = minecraft.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
        // 绘制回调只捕获已提取的不可变几何，不访问世界或模型缓存。
        event.addCustomRenderer((state, buffer, poses, translucentPass, levelState) -> {
            if (state.isTranslucent() != translucentPass) return true;
            Vec3 camera = levelState.cameraRenderState.pos;
            poses.pushPose();
            try {
                poses.translate(pos.getX() - camera.x + offset.x, pos.getY() - camera.y + offset.y, pos.getZ() - camera.z + offset.z);
                for (int pass = state.highContrast() ? 0 : 1; pass < 2; pass++) {
                    boolean secondary = pass == 0;
                    VertexConsumer consumer = buffer.getBuffer(secondary ? RenderTypes.secondaryBlockOutline() : RenderTypes.lines());
                    int color = secondary ? 0xFF000000 : state.highContrast() ? -11010079 : 0x66000000;
                    float red = (color >> 16 & 255) / 255F, green = (color >> 8 & 255) / 255F;
                    float blue = (color & 255) / 255F, alpha = (color >>> 24) / 255F;
                    float width = secondary ? 7.0F : lineWidth;
                    if (fallback != null) {
                        OutlineRenderer.render(poses, consumer, fallback, red, green, blue, alpha, SelectionGeometry.MAX_OUTLINE_SEGMENTS, width);
                    } else {
                        for (int i = 0; i < outlines.length; i++) {
                            poses.pushPose();
                            parts.get(i).apply(poses);
                            OutlineRenderer.render(poses, consumer, outlines[i], red, green, blue, alpha, SelectionGeometry.MAX_OUTLINE_SEGMENTS, width);
                            poses.popPose();
                        }
                    }
                    buffer.endLastBatch();
                }
            } finally {
                poses.popPose();
            }
            return true;
        });
    }

    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) { CubeSelection.clearWorld(); }
}
