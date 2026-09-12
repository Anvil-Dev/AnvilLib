package dev.anvilcraft.lib.v2.cube.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.cube.AnvilLibCube;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.anvilcraft.lib.v2.cube.geometry.PackedOutline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
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
        // 1.21.8 分两次发出事件，按原版规则只在对应的透明度阶段绘制。
        if (!level.getWorldBorder().isWithinBounds(pos)
            || net.neoforged.neoforge.client.ClientHooks.isInTranslucentBlockOutlinePass(level, pos, level.getBlockState(pos))
                != event.isForTranslucentBlocks()) return;
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(
            !level.tickRateManager().isEntityFrozen(event.getCamera().getEntity()));
        partial = CubeSelection.frameFraction(level, partial);
        CubeSelection.Target target = CubeSelection.target(level, pos, level.getBlockState(pos), partial);
        if (target == null) return;
        PoseStack stack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        Vec3 offset = target.offset();
        stack.pushPose();
        stack.translate(pos.getX() - camera.x + offset.x, pos.getY() - camera.y + offset.y, pos.getZ() - camera.z + offset.z);
        PackedOutline[] outlines = new PackedOutline[target.parts().size()];
        int segments = 0;
        for (int i = 0; i < outlines.length; i++) {
            outlines[i] = CubeSelection.outlines().get(target.parts().get(i).geometry());
            segments += outlines[i].segmentCount();
        }
        try {
            boolean highContrast = minecraft.options.highContrastBlockOutline().get();
            for (int pass = highContrast ? 0 : 1; pass < 2; pass++) {
                VertexConsumer consumer = event.getMultiBufferSource().getBuffer(
                    pass == 0 ? RenderType.secondaryBlockOutline() : RenderType.lines());
                int color = pass == 0 ? 0xFF000000 : highContrast ? -11010079 : 0x66000000;
                float red = (color >> 16 & 255) / 255F;
                float green = (color >> 8 & 255) / 255F;
                float blue = (color & 255) / 255F;
                float alpha = (color >>> 24) / 255F;
                if (segments > SelectionGeometry.MAX_OUTLINE_SEGMENTS && target.bounds() != null) {
                    ShapeRenderer.renderLineBox(stack, consumer, target.bounds(), red, green, blue, alpha);
                } else {
                    for (int i = 0; i < outlines.length; i++) {
                        stack.pushPose();
                        try {
                            target.parts().get(i).apply(stack);
                            OutlineRenderer.render(stack, consumer, outlines[i], red, green, blue, alpha);
                        } finally {
                            stack.popPose();
                        }
                    }
                }
                if (event.getMultiBufferSource() instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource source) {
                    source.endLastBatch();
                }
            }
        } finally {
            stack.popPose();
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) { CubeSelection.clearWorld(); }
}
