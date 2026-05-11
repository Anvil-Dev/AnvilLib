package dev.anvilcraft.lib.v2.rendering.cachedber.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface CachedBlockEntityRenderer<T extends BlockEntity, S extends CachedBlockEntityRenderState> {
    S createRenderState();

    S extractRenderState(T blockEntity, S state, float partialTicks, Camera camera);

    void submit(S renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera);
}
