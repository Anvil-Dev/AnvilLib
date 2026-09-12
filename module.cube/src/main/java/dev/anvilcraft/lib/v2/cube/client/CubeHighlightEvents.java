package dev.anvilcraft.lib.v2.cube.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.cube.AnvilLibCube;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.anvilcraft.lib.v2.cube.geometry.PackedOutline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

@EventBusSubscriber(modid = AnvilLibCube.MOD_ID, value = Dist.CLIENT)
public final class CubeHighlightEvents {
    private CubeHighlightEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void highlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.options.hideGui) return;
        BlockPos pos = event.getTarget().getBlockPos();
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(
            !level.tickRateManager().isEntityFrozen(event.getCamera().getEntity()));
        partial = CubeSelection.frameFraction(level, partial);
        CubeSelection.Target target = CubeSelection.target(level, pos, level.getBlockState(pos), partial);
        if (target == null) return;
        PoseStack stack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        Vec3 offset = target.offset();
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        stack.pushPose();
        stack.translate(pos.getX() - camera.x + offset.x, pos.getY() - camera.y + offset.y, pos.getZ() - camera.z + offset.z);
        PackedOutline[] outlines = new PackedOutline[target.parts().size()];
        int segments = 0;
        for (int i = 0; i < outlines.length; i++) {
            outlines[i] = CubeSelection.outlines().get(target.parts().get(i).geometry());
            segments += outlines[i].segmentCount();
        }
        if (segments > SelectionGeometry.MAX_OUTLINE_SEGMENTS && target.bounds() != null) {
            net.minecraft.client.renderer.ShapeRenderer.renderLineBox(stack, consumer, target.bounds(), 0, 0, 0, 0.4F);
        } else {
            for (int i = 0; i < outlines.length; i++) {
                stack.pushPose();
                target.parts().get(i).apply(stack);
                OutlineRenderer.render(stack, consumer, outlines[i], 0, 0, 0, 0.4F);
                stack.popPose();
            }
        }
        stack.popPose();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) { CubeSelection.clearWorld(); }
}
